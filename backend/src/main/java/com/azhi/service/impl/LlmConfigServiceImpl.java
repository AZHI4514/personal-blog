package com.azhi.service.impl;

import com.azhi.mapper.LifeMapper;
import com.azhi.pojo.LifeUser;
import com.azhi.pojo.LlmConfig;
import com.azhi.service.DynamicLLMService;
import com.azhi.service.LlmConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LlmConfigServiceImpl implements LlmConfigService {

    @Autowired
    private LifeMapper lifeMapper;

    @Autowired
    private TextEncryptor textEncryptor;

    @Autowired
    private DynamicLLMService dynamicLLMService;

    @Override
    @Transactional
    public void saveConfig(String deviceId, String baseUrl, String apiKey, String modelName, String customPrompt) {
        // 确保用户存在
        LifeUser user = lifeMapper.selectUserByDeviceId(deviceId);
        if (user == null) {
            user = new LifeUser();
            user.setDeviceId(deviceId);
            lifeMapper.insertUser(user);
        }

        // 使用默认模型名
        String effectiveModel = (modelName == null || modelName.isBlank()) ? "gpt-3.5-turbo" : modelName;

        // 检查是否已有配置
        LlmConfig existing = lifeMapper.selectLlmConfigByUserId(user.getId());

        // apiKey 处理：若含 **** 说明用户未修改 → 保留旧密钥
        String encryptedKey;
        if (apiKey != null && apiKey.contains("****") && existing != null) {
            encryptedKey = existing.getApiKey(); // 保留旧加密密钥
        } else {
            encryptedKey = textEncryptor.encrypt(apiKey);
        }

        LlmConfig config = new LlmConfig();
        config.setUserId(user.getId());
        config.setBaseUrl(baseUrl);
        config.setApiKey(encryptedKey);
        config.setModelName(effectiveModel);
        config.setCustomPrompt(customPrompt);

        if (existing != null) {
            lifeMapper.updateLlmConfig(config);
        } else {
            lifeMapper.insertLlmConfig(config);
        }

        // 清除模型缓存，下次使用新配置
        dynamicLLMService.evictModel(user.getId());
    }

    @Override
    public LlmConfig getConfig(String deviceId) {
        LifeUser user = lifeMapper.selectUserByDeviceId(deviceId);
        if (user == null) return null;

        LlmConfig config = lifeMapper.selectLlmConfigByUserId(user.getId());
        if (config == null) return null;

        // 解密 apiKey
        try {
            config.setApiKey(textEncryptor.decrypt(config.getApiKey()));
        } catch (Exception e) {
            // 解密失败（可能是旧数据或损坏），返回 null
            return null;
        }
        return config;
    }

    @Override
    public LlmConfig getMaskedConfig(String deviceId) {
        LifeUser user = lifeMapper.selectUserByDeviceId(deviceId);
        if (user == null) return null;

        LlmConfig config = lifeMapper.selectLlmConfigByUserId(user.getId());
        if (config == null) return null;

        // 先解密，再对明文脱敏：显示前6位 + **** + 后4位（如 sk-ant****x9y8）
        try {
            String decrypted = textEncryptor.decrypt(config.getApiKey());
            if (decrypted != null && decrypted.length() > 10) {
                config.setApiKey(decrypted.substring(0, 6) + "****" + decrypted.substring(decrypted.length() - 4));
            } else {
                config.setApiKey("****");
            }
        } catch (Exception e) {
            config.setApiKey("****");
        }
        return config;
    }

    @Override
    public LlmConfig getDecryptedConfigByUserId(Long userId) {
        LlmConfig config = lifeMapper.selectLlmConfigByUserId(userId);
        if (config == null) return null;

        // 解密 apiKey
        try {
            config.setApiKey(textEncryptor.decrypt(config.getApiKey()));
        } catch (Exception e) {
            return null;
        }
        return config;
    }

    @Override
    public String testConnection(String deviceId, String baseUrl, String apiKey, String modelName) {        try {
            String result = dynamicLLMService.testConnection(baseUrl, apiKey, modelName);
            if (result != null && result.toLowerCase().contains("ok")) {
                return "连接测试成功！模型返回: " + result;
            }
            return "连接测试完成，但模型返回异常: " + result;
        } catch (Exception e) {
            throw new RuntimeException("连接测试失败: " + e.getMessage(), e);
        }
    }
}
