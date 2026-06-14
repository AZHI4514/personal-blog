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

    // ==================== 系统提示词（极简，所有设定由玩家提供） ====================

    private static final String SYSTEM_PROMPT = """
        你是一个"人生模拟器"的剧情引擎。你必须严格以玩家的【自定义设定】作为世界观和规则的唯一依据。
        如果玩家没有提供设定，则自由发挥创意。

        角色属性（0-100）：money(金钱), health(健康), happiness(快乐), morality(道德), knowledge(知识)。
        当任一属性降至0或以下时，角色死亡。

        【重要规则】health(健康) 仅在剧情中明确涉及身体受伤、疾病、战斗、意外事故等直接伤害身体的
        事件时才可以扣除。日常剧情、社交活动、工作学习等不涉及身体伤害的场景，health 只能保持或增加，
        不得减少。

        每回合你收到：玩家设定 + 当前状态 + 故事历史 + 玩家选择。
        你需要做两件事：
        1. 判断该选择带来的属性变化（statChanges，每个属性 -20 到 +20）
        2. 生成接下来的一小段剧情（约100字）和2-4个新选项

        只返回JSON，不要任何解释或markdown标记：
        {"statChanges":{"money":0,"health":0,"happiness":0,"morality":0,"knowledge":0},"description":"剧情（约100字）","options":[{"text":"选项1"},{"text":"选项2"}]}
        """;

    // ==================== 公开方法 ====================

    @Override
    @Transactional
    public Map<String, Object> startGame(String deviceId, String name) {
        LlmConfig config = llmConfigService.getConfig(deviceId);
        if (config == null) {
            throw new IllegalStateException("请先配置大模型再开始游戏");
        }
        LifeUser user = ensureUser(deviceId);
        return doStartGame(user.getId(), name, config);
    }

    /** 创建全新角色并生成开局剧情 */
    private Map<String, Object> doStartGame(Long userId, String name, LlmConfig config) {
        LifeCharacter character = createFreshCharacter(userId, name);
        lifeMapper.insertCharacter(character);

        String userPrompt = buildStartPrompt(character);
        String aiResponse = callAI(config, userPrompt);
        Map<String, Object> story = parseAIResponse(aiResponse);

        saveEvent(character.getId(), character.getAge(), story, "游戏开始");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("character", character);
        result.put("story", story);
        return result;
    }

    /** 创建一个属性全默认的空白角色 */
    private LifeCharacter createFreshCharacter(Long userId, String name) {
        LifeCharacter c = new LifeCharacter();
        c.setUserId(userId);
        c.setName(name != null && !name.isBlank() ? name : "无名氏");
        c.setAge(0);
        c.setMoney(100);
        c.setHealth(80);
        c.setHappiness(60);
        c.setMorality(50);
        c.setKnowledge(30);
        c.setIsAlive(true);
        c.setGeneration(1);
        return c;
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

        String choiceText = (String) options.get(choiceIndex).get("text");

        // 获取故事历史（最近事件摘要）
        String storyHistory = buildStoryHistory(characterId);

        // 获取 LLM 配置
        LlmConfig llmConfig = llmConfigService.getDecryptedConfigByUserId(character.getUserId());

        // 让 AI 决定：属性变化 + 接下来的剧情 + 新选项
        String userPrompt = buildActionPrompt(character, choiceText, storyHistory);
        String aiResponse = callAI(llmConfig, userPrompt);
        Map<String, Object> story = parseAIResponse(aiResponse);

        // 应用 AI 决定的属性变化
        @SuppressWarnings("unchecked")
        Map<String, Object> statChanges = (Map<String, Object>) story.get("statChanges");
        if (statChanges != null) {
            applyEffects(character, statChanges);
        }

        // 游戏天数 +1
        character.setAge(character.getAge() + 1);
        lifeMapper.updateCharacter(character);

        // 检查角色是否死亡
        if (!Boolean.TRUE.equals(character.getIsAlive())) {
            saveEvent(character.getId(), character.getAge(),
                Map.of("description", "你的人生走到了尽头...", "options", List.of()),
                choiceText);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("character", character);
            result.put("story", Map.of(
                "description", "你的人生走到了尽头。第 " + character.getAge() + " 天。点击「重新开局」开始新的旅程。",
                "options", List.of(),
                "isGameOver", true
            ));
            result.put("lastChoice", choiceText);
            result.put("lastEffects", statChanges);
            return result;
        }

        // 清理 story 中的 statChanges（不存入事件）
        story.remove("statChanges");

        // 记录事件
        saveEvent(character.getId(), character.getAge(), story, choiceText);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("character", character);
        result.put("story", story);
        result.put("lastChoice", choiceText);
        result.put("lastEffects", statChanges);
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
            fullSystemPrompt += "\n\n【玩家自定义设定】\n" + config.getCustomPrompt();
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

    /** 构建最近事件的摘要，提供给 AI 作为上下文 */
    private String buildStoryHistory(Long characterId) {
        List<LifeEvent> events = lifeMapper.selectEventsByCharacterId(characterId, 0, 8);
        if (events.isEmpty()) return "（尚无剧情记录）";

        StringBuilder sb = new StringBuilder();
        for (int i = events.size() - 1; i >= 0; i--) {
            LifeEvent e = events.get(i);
            sb.append("第").append(e.getAge()).append("天：").append(e.getDescription());
            if (e.getChoiceMade() != null && !e.getChoiceMade().isBlank()) {
                sb.append(" → 选择：").append(e.getChoiceMade());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildStartPrompt(LifeCharacter c) {
        return String.format("""
            玩家名字是：%s

            角色初始状态（第1天）：
            - 金钱：%d | 健康：%d | 快乐：%d | 道德：%d | 知识：%d

            今天是游戏的第1天，请根据玩家设定生成开局剧情和2-4个选项。
            不需要返回statChanges（初始状态无需更新）。
            """,
            c.getName(), c.getMoney(), c.getHealth(),
            c.getHappiness(), c.getMorality(), c.getKnowledge()
        );
    }

    private String buildActionPrompt(LifeCharacter c, String choiceMade, String storyHistory) {
        return String.format("""
            玩家名字是：%s

            当前是第 %d 天。
            角色当前属性：
            - 金钱：%d | 健康：%d | 快乐：%d | 道德：%d | 知识：%d

            玩家刚才选择了：「%s」

            之前的剧情摘要：
            %s

            请根据以上信息和玩家设定：
            1. 判断这个选择带来了什么后果（更新statChanges）
            2. 生成接下来的剧情和2-4个新选项

            注意：statChanges 表示该选择导致的属性变化。可以有0值（无变化）。
            """,
            c.getName(), c.getAge(), c.getMoney(), c.getHealth(),
            c.getHappiness(), c.getMorality(), c.getKnowledge(),
            choiceMade, storyHistory
        );
    }


    private Map<String, Object> parseAIResponse(String aiResponse) {
        try {
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

    /** 确保解析后的对象有 statChanges、description 和 options 字段 */
    private Map<String, Object> validateAndFix(Map<String, Object> parsed) {
        if (!parsed.containsKey("description") || String.valueOf(parsed.get("description")).isBlank()) {
            parsed.put("description", "新的一天开始了，你站在十字路口，思考着接下来该做什么。");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) parsed.get("options");
        if (options == null || options.isEmpty()) {
            parsed.put("options", List.of(
                Map.of("text", "勇敢尝试新事物"),
                Map.of("text", "保持现状"),
                Map.of("text", "另辟蹊径"),
                Map.of("text", "独自冥想")
            ));
        }
        if (!parsed.containsKey("statChanges")) {
            parsed.put("statChanges", Map.of(
                "money", 0, "health", 0, "happiness", 0, "morality", 0, "knowledge", 0
            ));
        }
        return parsed;
    }

    private Map<String, Object> buildFallback() {
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("statChanges", Map.of(
            "money", 0, "health", 0, "happiness", 5, "morality", 0, "knowledge", 2
        ));
        fallback.put("description", randomFallbackDescription());
        fallback.put("options", List.of(
            Map.of("text", "勇敢闯荡"),
            Map.of("text", "安分守己"),
            Map.of("text", "结交好友"),
            Map.of("text", "独自冥想")
        ));
        return fallback;
    }

    private String randomFallbackDescription() {
        String[] descs = {
            "新的一天开始了，你感到生活中充满了可能。站在人生的十字路口，你将何去何从？",
            "平淡的日子里也藏着惊喜。一封意外的来信、街头偶遇的老朋友，都让这一天变得不同寻常。",
            "微风拂过面庞，过去的选择塑造了现在的你，而未来的路还在脚下延伸。",
            "夜深人静时，你翻看着日记本，回顾这些天的点点滴滴。有欢笑也有泪水，但每一天都是真实的。"
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
