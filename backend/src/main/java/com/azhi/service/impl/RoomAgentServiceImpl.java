package com.azhi.service.impl;

import com.azhi.service.RoomAgentService;
import com.azhi.service.roomagent.RoomAgentChatService;
import com.azhi.service.roomagent.RoomAgentConfigService;
import com.azhi.service.roomagent.RoomAgentMemoryService;
import com.azhi.service.roomagent.RoomAgentWorldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomAgentServiceImpl implements RoomAgentService {

    private final RoomAgentWorldService worldService;
    private final RoomAgentMemoryService memoryService;
    private final RoomAgentConfigService configService;
    private final RoomAgentChatService chatService;

    @Override
    public Map<String, Object> buildWorld(Double lat, Double lon, String timezone) {
        return worldService.buildWorld(lat, lon, timezone);
    }

    @Override
    public List<Map<String, Object>> listMemories(String userId, String type, int limit) {
        return memoryService.listMemories(userId, type, limit);
    }

    @Override
    public List<Map<String, Object>> searchMemories(String userId, String query, int limit) {
        return memoryService.searchMemories(userId, query, limit);
    }

    @Override
    public Map<String, Object> recordMemory(String userId, Map<String, Object> payload) {
        return memoryService.recordMemory(userId, payload);
    }

    @Override
    public Map<String, Object> updateMemory(String userId, String id, Map<String, Object> payload) {
        return memoryService.updateMemory(userId, id, payload);
    }

    @Override
    public int deleteMemory(String userId, String id) {
        return memoryService.deleteMemory(userId, id);
    }

    @Override
    public Map<String, Object> callAllowedTool(Map<String, Object> body) {
        return chatService.callAllowedTool(body);
    }

    @Override
    public Map<String, Object> getConfigView() {
        return configService.getConfigView();
    }

    @Override
    public Map<String, Object> saveConfig(String adminUser, Map<String, Object> body) {
        return configService.saveConfig(adminUser, body);
    }

    @Override
    public Map<String, Object> chat(Map<String, Object> body) {
        return chatService.chat(body);
    }
}
