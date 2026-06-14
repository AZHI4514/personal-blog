package com.azhi.service.impl;

import com.azhi.mapper.LifeMapper;
import com.azhi.pojo.*;
import com.azhi.service.DynamicLLMService;
import com.azhi.service.LifeService;
import com.azhi.service.LlmConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LifeServiceImpl implements LifeService {

    private static final Logger log = LoggerFactory.getLogger(LifeServiceImpl.class);

    @Autowired
    private LifeMapper lifeMapper;

    @Autowired
    private LlmConfigService llmConfigService;

    @Autowired
    private DynamicLLMService dynamicLLMService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 系统提示词 ====================

    private static final String SYSTEM_PROMPT = """
        你是一个"人生模拟器"的剧情引擎，负责为玩家生成精彩的人生故事。

        角色拥有5个属性（0-100）：money(金钱), health(健康), happiness(快乐), morality(道德), knowledge(知识)。
        每回合玩家做出选择后，属性会相应增减，然后进入下一岁。

        你的任务：根据角色当前状态，生成一段引人入胜的剧情（150-300字），并提供2-4个有意义的选择。
        剧情风格参考《模拟人生》+《中国式家长》，可以温馨、搞笑、戏剧化、偶尔荒诞。
        必须使用第二人称"你"来叙述。15%%概率触发随机事件（彩票中奖、街头遇险、神秘来信等）。

        只返回以下JSON，不要任何解释、问候或markdown标记：
        {"description":"剧情文本","options":[{"text":"选项1","effects":{"money":0,"health":0,"happiness":0,"morality":0,"knowledge":0}}]}

        effects数值在-15到+15之间，总和正负平衡。当任一属性≤0时角色死亡。
        """;

    // ==================== 公开方法 ====================

    @Override
    @Transactional
    public Map<String, Object> startGame(String deviceId, String name) {
        // 获取用户 LLM 配置
        LlmConfig config = llmConfigService.getConfig(deviceId);
        if (config == null) {
            throw new IllegalStateException("请先配置大模型再开始游戏");
        }

        // 查找或创建用户
        LifeUser user = ensureUser(deviceId);

        // 创建角色
        LifeCharacter character = new LifeCharacter();
        character.setUserId(user.getId());
        character.setName(name != null && !name.isBlank() ? name : "无名氏");
        character.setAge(0);
        character.setMoney(100);
        character.setHealth(80);
        character.setHappiness(60);
        character.setMorality(50);
        character.setKnowledge(30);
        character.setIsAlive(true);
        character.setGeneration(1);
        lifeMapper.insertCharacter(character);

        // 生成首段剧情
        String userPrompt = buildStartPrompt(character);
        String aiResponse = callAI(config, userPrompt);
        Map<String, Object> story = parseAIResponse(aiResponse, character);

        // 记录事件
        saveEvent(character.getId(), character.getAge(), story, "游戏开始");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("character", character);
        result.put("story", story);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> processAction(Long characterId, Integer choiceIndex) {
        LifeCharacter character = lifeMapper.selectCharacterById(characterId);
        if (character == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        if (!Boolean.TRUE.equals(character.getIsAlive())) {
            throw new IllegalStateException("角色已死亡，请重新开局");
        }

        // 获取最后一条事件，解析出选项
        List<LifeEvent> recentEvents = lifeMapper.selectEventsByCharacterId(characterId, 0, 1);
        if (recentEvents.isEmpty()) {
            throw new IllegalStateException("没有找到之前的剧情，请重新开始游戏");
        }

        LifeEvent lastEvent = recentEvents.get(0);
        Map<String, Object> lastStory = parseJson(lastEvent.getEffects());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) lastStory.get("options");

        if (options == null || choiceIndex < 0 || choiceIndex >= options.size()) {
            throw new IllegalArgumentException("无效的选择");
        }

        Map<String, Object> chosen = options.get(choiceIndex);
        @SuppressWarnings("unchecked")
        Map<String, Object> effects = (Map<String, Object>) chosen.get("effects");

        // 应用效果到角色属性
        if (effects != null) {
            applyEffects(character, effects);
        }

        // 年龄 +1
        character.setAge(character.getAge() + 1);
        lifeMapper.updateCharacter(character);

        // 检查角色是否死亡
        if (!Boolean.TRUE.equals(character.getIsAlive())) {
            saveEvent(character.getId(), character.getAge(),
                Map.of("description", "你的人生走到了尽头...", "options", List.of()),
                (String) chosen.get("text"));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("character", character);
            result.put("story", Map.of(
                "description", "你的人生走到了尽头。享年 " + character.getAge() + " 岁。点击「重新开局」开始新的旅程。",
                "options", List.of(),
                "isGameOver", true
            ));
            result.put("lastChoice", chosen.get("text"));
            result.put("lastEffects", effects);
            return result;
        }

        // 获取用户 LLM 配置（通过 LlmConfigService 解密 apiKey）
        LlmConfig llmConfig = llmConfigService.getDecryptedConfigByUserId(character.getUserId());

        // 生成下一段剧情
        String userPrompt = buildActionPrompt(character, (String) chosen.get("text"), effects);
        String aiResponse = callAI(llmConfig, userPrompt);
        Map<String, Object> story = parseAIResponse(aiResponse, character);

        // 记录事件
        saveEvent(character.getId(), character.getAge(), story, (String) chosen.get("text"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("character", character);
        result.put("story", story);
        result.put("lastChoice", chosen.get("text"));
        result.put("lastEffects", effects);
        return result;
    }

    @Override
    public LifeCharacter getCharacterState(Long characterId) {
        LifeCharacter character = lifeMapper.selectCharacterById(characterId);
        if (character == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        return character;
    }

    @Override
    public List<LifeEvent> getEvents(Long characterId, int page, int size) {
        int offset = Math.max(0, page - 1) * size;
        return lifeMapper.selectEventsByCharacterId(characterId, offset, size);
    }

    @Override
    @Transactional
    public Map<String, Object> resetGame(Long characterId) {
        LifeCharacter old = lifeMapper.selectCharacterById(characterId);
        if (old == null) {
            throw new IllegalArgumentException("角色不存在");
        }

        // 杀死旧角色
        lifeMapper.killCharacter(characterId);

        // 创建新角色，继承部分成就
        LifeCharacter character = new LifeCharacter();
        character.setUserId(old.getUserId());
        character.setName(old.getName());
        character.setAge(0);
        // 继承：金钱保留20%，知识保留30%，其他重置
        character.setMoney(Math.min(100, (int)(old.getMoney() * 0.2) + 50));
        character.setHealth(80);
        character.setHappiness(60);
        character.setMorality(50);
        character.setKnowledge(Math.min(100, (int)(old.getKnowledge() * 0.3) + 20));
        character.setIsAlive(true);
        character.setGeneration(old.getGeneration() + 1);
        lifeMapper.insertCharacter(character);

        // 获取 LLM 配置（通过 LlmConfigService 解密 apiKey）
        LlmConfig config = llmConfigService.getDecryptedConfigByUserId(old.getUserId());

        // 生成首段剧情（轮回转世风格）
        String userPrompt = buildRebirthPrompt(character);
        String aiResponse = callAI(config, userPrompt);
        Map<String, Object> story = parseAIResponse(aiResponse, character);

        saveEvent(character.getId(), character.getAge(), story, "重新开局（第" + character.getGeneration() + "代）");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("character", character);
        result.put("story", story);
        result.put("previousGeneration", old.getGeneration());
        return result;
    }

    @Override
    public boolean hasLlmConfig(String deviceId) {
        return llmConfigService.getConfig(deviceId) != null;
    }

    @Override
    @Transactional
    public void deleteCharacter(Long characterId) {
        lifeMapper.deleteEventsByCharacterId(characterId);
        lifeMapper.deleteCharacter(characterId);
    }

    @Override
    @Transactional
    public void deleteUserData(String deviceId) {
        LifeUser user = lifeMapper.selectUserByDeviceId(deviceId);
        if (user == null) return;

        // 删除顺序：事件 → 角色 → LLM配置 → 用户
        lifeMapper.deleteEventsByUserId(user.getId());
        lifeMapper.deleteCharactersByUserId(user.getId());
        lifeMapper.deleteLlmConfigByUserId(user.getId());
        lifeMapper.deleteUserById(user.getId());
    }

    // ==================== 私有辅助方法 ====================

    private LifeUser ensureUser(String deviceId) {
        LifeUser user = lifeMapper.selectUserByDeviceId(deviceId);
        if (user == null) {
            user = new LifeUser();
            user.setDeviceId(deviceId);
            lifeMapper.insertUser(user);
        }
        return user;
    }

    private String callAI(LlmConfig config, String userPrompt) {
        if (config == null) {
            throw new IllegalStateException("大模型配置不存在");
        }
        // 拼接系统提示词 + 用户自定义风格
        String fullSystemPrompt = SYSTEM_PROMPT;
        if (config.getCustomPrompt() != null && !config.getCustomPrompt().isBlank()) {
            fullSystemPrompt += "\n【站主自定义风格要求】\n" + config.getCustomPrompt();
        }
        try {
            return dynamicLLMService.generate(
                config.getUserId(), config.getBaseUrl(), config.getApiKey(),
                config.getModelName(), fullSystemPrompt, userPrompt
            );
        } catch (Exception e) {
            throw new RuntimeException("AI 生成剧情超时（3秒），请检查大模型配置或重试: " + e.getMessage(), e);
        }
    }

    private String buildStartPrompt(LifeCharacter c) {
        return String.format("""
            玩家名字是：%s

            角色初始状态：
            - 年龄：%d岁（新生儿）
            - 金钱：%d | 健康：%d | 快乐：%d | 道德：%d | 知识：%d

            这是角色的第一段人生剧情。请从角色的出生开始描述，给出有趣的童年开局剧情和2-4个选项。
            """,
            c.getName(), c.getAge(), c.getMoney(), c.getHealth(),
            c.getHappiness(), c.getMorality(), c.getKnowledge()
        );
    }

    private String buildActionPrompt(LifeCharacter c, String choiceMade, Map<String, Object> effects) {
        return String.format("""
            玩家名字是：%s

            角色当前状态：
            - 年龄：%d岁
            - 金钱：%d | 健康：%d | 快乐：%d | 道德：%d | 知识：%d

            上一回合，玩家选择了：「%s」
            属性变化：%s

            请根据以上信息，生成下一段人生剧情和2-4个选项。偶尔可以触发随机事件（意外中奖、生病、遇险等）。
            """,
            c.getName(), c.getAge(), c.getMoney(), c.getHealth(),
            c.getHappiness(), c.getMorality(), c.getKnowledge(),
            choiceMade, effects != null ? effects.toString() : "无变化"
        );
    }

    private String buildRebirthPrompt(LifeCharacter c) {
        return String.format("""
            玩家名字是：%s

            角色初始状态：
            - 年龄：%d岁
            - 金钱：%d | 健康：%d | 快乐：%d | 道德：%d | 知识：%d
            - 这是第 %d 代转世

            这是角色的转世开局剧情。请用一种带有"前世记忆碎片"或"轮回转世"风格的描述开始新的旅程，给出2-4个选项。
            """,
            c.getName(), c.getAge(), c.getMoney(), c.getHealth(),
            c.getHappiness(), c.getMorality(), c.getKnowledge(),
            c.getGeneration()
        );
    }

    private Map<String, Object> parseAIResponse(String aiResponse, LifeCharacter character) {
        try {
            // 使用括号计数法提取完整 JSON（处理嵌套对象）
            String json = extractJson(aiResponse);
            log.debug("Extracted JSON: {}", json.length() > 200 ? json.substring(0, 200) + "..." : json);
            Map<String, Object> parsed = objectMapper.readValue(json,
                new TypeReference<Map<String, Object>>() {});
            return validateAndFix(parsed);
        } catch (Exception e) {
            log.warn("AI response parse failed, using fallback. Raw: {}",
                aiResponse != null ? aiResponse.substring(0, Math.min(200, aiResponse.length())) : "null");
            return buildFallback();
        }
    }

    /**
     * 用括号计数法提取第一个完整的 JSON 对象。
     * 先尝试去除 markdown 代码块，然后找到最外层 { ... } 块。
     */
    private String extractJson(String text) {
        if (text == null || text.isBlank()) return "{}";

        // 先去除 markdown 代码块标记
        String clean = text
            .replaceAll("```json\\s*", "")
            .replaceAll("```\\s*", "");

        // 找到第一个 { 和匹配的 }
        int start = clean.indexOf('{');
        if (start < 0) return "{}";

        int depth = 0;
        for (int i = start; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            if (depth == 0) {
                return clean.substring(start, i + 1).trim();
            }
        }
        // 括号未闭合，返回从 start 到末尾
        return clean.substring(start).trim();
    }

    /** 确保解析后的对象有 description 和 options 字段 */
    private Map<String, Object> validateAndFix(Map<String, Object> parsed) {
        if (!parsed.containsKey("description") || String.valueOf(parsed.get("description")).isBlank()) {
            parsed.put("description", "时光流转，你的生活翻开了新的一页。");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) parsed.get("options");
        if (options == null || options.isEmpty()) {
            parsed.put("options", List.of(
                Map.of("text", "勇敢尝试", "effects", Map.of("money", 5, "happiness", 5)),
                Map.of("text", "保持现状", "effects", Map.of("health", 3, "knowledge", 3)),
                Map.of("text", "另辟蹊径", "effects", Map.of("morality", 5, "money", -2))
            ));
        }
        return parsed;
    }

    private Map<String, Object> buildFallback() {
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("description", randomFallbackDescription());
        fallback.put("options", List.of(
            Map.of("text", "勇敢闯荡", "effects", Map.of("money", 10, "health", -5, "happiness", 5)),
            Map.of("text", "安分守己", "effects", Map.of("money", 3, "health", 5, "knowledge", 5)),
            Map.of("text", "结交好友", "effects", Map.of("happiness", 10, "morality", 3)),
            Map.of("text", "独自冥想", "effects", Map.of("knowledge", 8, "happiness", -2))
        ));
        return fallback;
    }

    private String randomFallbackDescription() {
        String[] descs = {
            "这一年的时光如白驹过隙，你感到生活中充满了可能。站在人生的十字路口，你将何去何从？",
            "平淡的日子里也藏着惊喜。一封意外的来信、街头偶遇的老朋友，都让这一年变得不同寻常。",
            "微风拂过面庞，你意识到自己又长大了一岁。过去的选择塑造了现在的你，而未来的路还在脚下延伸。",
            "夜深人静时，你翻看着日记本，回顾这一年的点点滴滴。有欢笑也有泪水，但每一天都是真实的。"
        };
        return descs[new Random().nextInt(descs.length)];
    }

    @SuppressWarnings("unchecked")
    private void applyEffects(LifeCharacter character, Map<String, Object> effects) {
        Map<String, Integer> statMap = new LinkedHashMap<>();
        statMap.put("money", character.getMoney());
        statMap.put("health", character.getHealth());
        statMap.put("happiness", character.getHappiness());
        statMap.put("morality", character.getMorality());
        statMap.put("knowledge", character.getKnowledge());

        for (Map.Entry<String, Object> entry : effects.entrySet()) {
            String key = entry.getKey();
            if (statMap.containsKey(key)) {
                try {
                    int delta = ((Number) entry.getValue()).intValue();
                    statMap.put(key, statMap.get(key) + delta);
                } catch (Exception ignored) {}
            }
        }

        // 限定范围 0-100，如果任何属性 <=0 → 死亡
        boolean dead = false;
        for (Map.Entry<String, Integer> entry : statMap.entrySet()) {
            int value = Math.max(0, Math.min(100, entry.getValue()));
            if (value <= 0) dead = true;
            statMap.put(entry.getKey(), value);
        }

        character.setMoney(statMap.get("money"));
        character.setHealth(statMap.get("health"));
        character.setHappiness(statMap.get("happiness"));
        character.setMorality(statMap.get("morality"));
        character.setKnowledge(statMap.get("knowledge"));
        if (dead) {
            character.setIsAlive(false);
        }
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private void saveEvent(Long characterId, Integer age, Map<String, Object> story, String choiceMade) {
        LifeEvent event = new LifeEvent();
        event.setCharacterId(characterId);
        event.setAge(age);
        event.setDescription((String) story.getOrDefault("description", ""));
        event.setChoiceMade(choiceMade);
        try {
            event.setEffects(objectMapper.writeValueAsString(story));
        } catch (Exception e) {
            event.setEffects("{}");
        }
        lifeMapper.insertEvent(event);
    }
}
