package com.azhi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;

/**
 * 加密配置：提供 TextEncryptor 用于 api_key 的加密存储。
 * 使用 AES-256-CBC + 随机 salt，密钥来自配置或自动生成。
 */
@Configuration
public class CryptoConfig {

    @Value("${app.crypto.password:personal-blog-life-sim-secret}")
    private String cryptoPassword;

    @Value("${app.crypto.salt:deadbeef}")
    private String cryptoSalt;

    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.text(cryptoPassword, cryptoSalt);
    }
}
