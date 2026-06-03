package com.azhi.service;

import java.util.List;
import java.util.Map;

public interface RoomAgentService {
    Map<String, Object> buildWorld(Double lat, Double lon, String timezone);
    
    List<Map<String, Object>> listMemories(String userId, String type, int limit);
    
    List<Map<String, Object>> searchMemories(String userId, String query, int limit);
    
    Map<String, Object> recordMemory(String userId, Map<String, Object> payload);
    
    Map<String, Object> updateMemory(String userId, String id, Map<String, Object> payload);
    
    int deleteMemory(String userId, String id);
    
    Map<String, Object> callAllowedTool(Map<String, Object> body);
    
    Map<String, Object> getConfigView();
    
    Map<String, Object> saveConfig(String adminUser, Map<String, Object> body);
    
    Map<String, Object> chat(Map<String, Object> body);
}
