package com.azhi.service;

import com.azhi.pojo.LifeCharacter;
import com.azhi.pojo.LifeEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface LifeService {

    /**
     * 初始化新角色并生成首段剧情。
     * @return { character: LifeCharacter, story: { description, options }, events: [...] }
     */
    Map<String, Object> startGame(String deviceId, String name);

    /**
     * 提交用户选择，返回下一段剧情。
     * @return { character: LifeCharacter, story: { description, options }, event: LifeEvent }
     */
    Map<String, Object> processAction(Long characterId, Integer choiceIndex);

    /**
     * 流式开始游戏：AI 生成文本通过 onText 回调实时推送，完成后调用 onDone。
     * onText 接收原始 AI 输出的文本片段；onDone 接收最终的结构化结果。
     */
    void startGameStream(String deviceId, String name,
                         Consumer<String> onText, Consumer<Map<String, Object>> onDone);

    /**
     * 流式提交选择：AI 生成文本通过 onText 回调实时推送，完成后调用 onDone。
     */
    void processActionStream(Long characterId, Integer choiceIndex,
                             Consumer<String> onText, Consumer<Map<String, Object>> onDone);

    /**
     * 获取当前角色状态。
     */
    LifeCharacter getCharacterState(Long characterId);

    /**
     * 获取事件历史（分页）。
     */
    List<LifeEvent> getEvents(Long characterId, int page, int size);

    /**
     * 检查用户是否已配置 LLM。
     */
    boolean hasLlmConfig(String deviceId);

    /**
     * 删除指定角色及其所有事件。
     */
    void deleteCharacter(Long characterId);

    /**
     * 删除用户的所有数据（配置、角色、事件、用户记录）。
     */
    void deleteUserData(String deviceId);
}
