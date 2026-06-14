package com.azhi.service;

import com.azhi.pojo.LifeCharacter;
import com.azhi.pojo.LifeEvent;

import java.util.List;
import java.util.Map;

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
