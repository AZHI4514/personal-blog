package com.azhi.controller;

import com.azhi.pojo.LifeActionRequest;
import com.azhi.pojo.LifeCharacter;
import com.azhi.pojo.LifeEvent;
import com.azhi.pojo.LlmConfig;
import com.azhi.pojo.Result;
import com.azhi.service.LifeService;
import com.azhi.service.LlmConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/life")
public class LifeController {

    @Autowired
    private LifeService lifeService;

    @Autowired
    private LlmConfigService llmConfigService;

    // ==================== LLM 配置 ====================

    /**
     * 保存/更新用户的大模型配置。
     */
    @PostMapping("/llm/config")
    public Result<String> saveLlmConfig(@RequestBody Map<String, String> body) {
        String deviceId = body.get("deviceId");
        String baseUrl = body.get("baseUrl");
        String apiKey = body.get("apiKey");
        String modelName = body.getOrDefault("modelName", "gpt-3.5-turbo");
        String customPrompt = body.get("customPrompt");

        if (deviceId == null || deviceId.isBlank()) {
            return Result.error("deviceId 不能为空");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return Result.error("baseUrl 不能为空");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return Result.error("apiKey 不能为空");
        }

        try {
            llmConfigService.saveConfig(deviceId, baseUrl, apiKey, modelName, customPrompt);
            return Result.success("配置保存成功");
        } catch (Exception e) {
            return Result.error("配置保存失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的大模型配置（脱敏）。
     */
    @GetMapping("/llm/config")
    public Result<Map<String, Object>> getLlmConfig(@RequestParam String deviceId) {
        LlmConfig config = llmConfigService.getMaskedConfig(deviceId);
        if (config == null) {
            return Result.success(Map.of("configured", false));
        }
        return Result.success(Map.of(
            "configured", true,
            "baseUrl", config.getBaseUrl(),
            "apiKey", config.getApiKey(), // 已脱敏
            "modelName", config.getModelName(),
            "customPrompt", config.getCustomPrompt() != null ? config.getCustomPrompt() : ""
        ));
    }

    /**
     * 测试用户的 LLM 配置是否可用。
     */
    @PostMapping("/llm/test")
    public Result<String> testLlmConnection(@RequestBody Map<String, String> body) {
        String deviceId = body.getOrDefault("deviceId", "test");
        String baseUrl = body.get("baseUrl");
        String apiKey = body.get("apiKey");
        String modelName = body.getOrDefault("modelName", "gpt-3.5-turbo");

        if (baseUrl == null || baseUrl.isBlank()) {
            return Result.error("baseUrl 不能为空");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return Result.error("apiKey 不能为空");
        }

        try {
            String result = llmConfigService.testConnection(deviceId, baseUrl, apiKey, modelName);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("连接测试失败: " + e.getMessage());
        }
    }

    // ==================== 游戏操作 ====================

    /**
     * 初始化新角色并返回首段剧情。
     */
    @PostMapping("/start")
    public Result<Map<String, Object>> startGame(@RequestBody Map<String, String> body) {
        String deviceId = body.get("deviceId");
        String name = body.getOrDefault("name", "无名氏");

        if (deviceId == null || deviceId.isBlank()) {
            return Result.error("deviceId 不能为空");
        }

        try {
            Map<String, Object> result = lifeService.startGame(deviceId, name);
            return Result.success(result);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("游戏启动失败: " + e.getMessage());
        }
    }

    /**
     * 提交用户选择，返回下一段剧情。
     */
    @PostMapping("/action")
    public Result<Map<String, Object>> processAction(@RequestBody LifeActionRequest request) {
        if (request.getCharacterId() == null) {
            return Result.error("characterId 不能为空");
        }
        if (request.getChoiceIndex() == null) {
            return Result.error("choiceIndex 不能为空");
        }

        try {
            Map<String, Object> result = lifeService.processAction(request.getCharacterId(), request.getChoiceIndex());
            return Result.success(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前角色状态。
     */
    @GetMapping("/state")
    public Result<LifeCharacter> getCharacterState(@RequestParam Long characterId) {
        try {
            LifeCharacter character = lifeService.getCharacterState(characterId);
            return Result.success(character);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取事件历史（分页）。
     */
    @GetMapping("/events")
    public Result<Map<String, Object>> getEvents(
        @RequestParam Long characterId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            List<LifeEvent> events = lifeService.getEvents(characterId, page, size);
            return Result.success(Map.of(
                "list", events,
                "page", page,
                "size", size
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 删除操作 ====================

    /**
     * 删除指定角色及其所有事件。
     */
    @DeleteMapping("/character")
    public Result<String> deleteCharacter(@RequestParam Long characterId) {
        try {
            lifeService.deleteCharacter(characterId);
            return Result.success("角色及事件已删除");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户的所有数据（LLM 配置 + 角色 + 事件 + 用户记录）。
     */
    @DeleteMapping("/user/data")
    public Result<String> deleteUserData(@RequestParam String deviceId) {
        try {
            lifeService.deleteUserData(deviceId);
            return Result.success("用户数据已全部删除");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
