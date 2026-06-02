package com.azhi.service;

import com.azhi.service.roomagent.RoomAgentChatService;
import com.azhi.service.roomagent.RoomAgentConfigService;
import com.azhi.service.roomagent.RoomAgentMemoryService;
import com.azhi.service.roomagent.RoomAgentWorldService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RoomAgentService {

    private final RoomAgentWorldService worldService;
    private final RoomAgentMemoryService memoryService;
    private final RoomAgentConfigService configService;
    private final RoomAgentChatService chatService;

    public RoomAgentService(
            RoomAgentWorldService worldService,
            RoomAgentMemoryService memoryService,
            RoomAgentConfigService configService,
            RoomAgentChatService chatService
    ) {
        this.worldService = worldService;
        this.memoryService = memoryService;
        this.configService = configService;
        this.chatService = chatService;
    }

    public Map<String, Object> buildWorld(Double lat, Double lon, String timezone) {
        return worldService.buildWorld(lat, lon, timezone);
    }

    public List<Map<String, Object>> listMemories(String userId, String type, int limit) {
        return memoryService.listMemories(userId, type, limit);
    }

    public List<Map<String, Object>> searchMemories(String userId, String query, int limit) {
        return memoryService.searchMemories(userId, query, limit);
    }

    public Map<String, Object> recordMemory(String userId, Map<String, Object> payload) {
        return memoryService.recordMemory(userId, payload);
    }

    public Map<String, Object> updateMemory(String userId, String id, Map<String, Object> payload) {
        return memoryService.updateMemory(userId, id, payload);
    }

    public int deleteMemory(String userId, String id) {
        return memoryService.deleteMemory(userId, id);
    }

    public Map<String, Object> callAllowedTool(Map<String, Object> body) {
        return chatService.callAllowedTool(body);
    }

    public Map<String, Object> getConfigView() {
        return configService.getConfigView();
    }

    public Map<String, Object> saveConfig(String adminUser, Map<String, Object> body) {
        return configService.saveConfig(adminUser, body);
    }

    public Map<String, Object> chat(Map<String, Object> body) {
        return chatService.chat(body);
    }
}
