package com.azhi.service.roomagent;

import com.azhi.mapper.RoomAgentConfigMapper;
import com.azhi.pojo.RoomAgentConfig;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RoomAgentConfigService {

    private static final String DEFAULT_CONFIG_KEY = "default";
    private final RoomAgentConfigMapper roomAgentConfigMapper;

    public RoomAgentConfigService(RoomAgentConfigMapper roomAgentConfigMapper) {
        this.roomAgentConfigMapper = roomAgentConfigMapper;
    }

    public Map<String, Object> getConfigView() {
        RoomAgentConfig config = getOrCreateDefaultConfig();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("apiUrl", safe(config.getApiUrl()));
        data.put("apiKey", safe(config.getApiKey()));
        data.put("model", safe(config.getModel()));
        data.put("visionMode", safe(config.getVisionMode(), "auto"));
        data.put("hasApiKey", config.getApiKey() != null && !config.getApiKey().isBlank());
        data.put("updatedBy", safe(config.getUpdatedBy()));
        data.put("updateTime", config.getUpdateTime());
        return data;
    }

    public Map<String, Object> saveConfig(String adminUser, Map<String, Object> body) {
        if (!"AZHI4514".equals(adminUser)) {
            throw new IllegalArgumentException("only admin can update config");
        }
        RoomAgentConfig current = getOrCreateDefaultConfig();
        current.setApiUrl(String.valueOf(body.getOrDefault("apiUrl", current.getApiUrl())));
        current.setApiKey(String.valueOf(body.getOrDefault("apiKey", current.getApiKey())));
        current.setModel(String.valueOf(body.getOrDefault("model", current.getModel())));
        current.setVisionMode(String.valueOf(body.getOrDefault("visionMode", current.getVisionMode() == null ? "auto" : current.getVisionMode())));
        current.setUpdatedBy(adminUser);
        int updated = roomAgentConfigMapper.updateByConfigKey(current);
        if (updated <= 0) {
            roomAgentConfigMapper.insert(current);
        }
        return getConfigView();
    }

    public RoomAgentConfig getCurrentConfig() {
        return getOrCreateDefaultConfig();
    }

    private RoomAgentConfig getOrCreateDefaultConfig() {
        RoomAgentConfig config = roomAgentConfigMapper.findByConfigKey(DEFAULT_CONFIG_KEY);
        if (config != null) {
            return config;
        }
        RoomAgentConfig fallback = new RoomAgentConfig();
        fallback.setConfigKey(DEFAULT_CONFIG_KEY);
        fallback.setApiUrl("");
        fallback.setApiKey("");
        fallback.setModel("");
        fallback.setVisionMode("auto");
        fallback.setUpdatedBy("AZHI4514");
        return fallback;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
