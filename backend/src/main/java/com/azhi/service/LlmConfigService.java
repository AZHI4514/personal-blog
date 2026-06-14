package com.azhi.service;

import com.azhi.pojo.LlmConfig;

/**
 * LLM 配置管理服务：管理用户大模型配置的加密存储、解密读取与验证。
 */
public interface LlmConfigService {

    /**
     * 保存或更新用户的大模型配置。
     * apiKey 会在存储前加密。
     */
    void saveConfig(String deviceId, String baseUrl, String apiKey, String modelName, String customPrompt);

    /**
     * 获取用户的大模型配置（apiKey 已解密）。
     * 返回 null 表示用户尚未配置。
     */
    LlmConfig getConfig(String deviceId);

    /**
     * 获取脱敏后的配置信息（apiKey 仅显示前 4 后 4 字符）。
     */
    LlmConfig getMaskedConfig(String deviceId);

    /**
     * 根据 userId 获取解密后的完整配置（供 LifeServiceImpl 内部使用）。
     * @return 解密后的 LlmConfig，或 null
     */
    LlmConfig getDecryptedConfigByUserId(Long userId);

    /**
     * 测试用户提供的大模型配置是否可用。
     * @return 测试结果描述
     */
    String testConnection(String deviceId, String baseUrl, String apiKey, String modelName);
}
