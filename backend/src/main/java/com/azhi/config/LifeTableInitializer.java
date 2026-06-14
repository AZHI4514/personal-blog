package com.azhi.config;

import com.azhi.mapper.LifeMapper;
import org.apache.ibatis.annotations.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时自动创建人生模拟器所需的数据库表（IF NOT EXISTS，幂等安全），
 * 并迁移旧表结构（添加缺失的列）。
 */
@Component
public class LifeTableInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LifeTableInitializer.class);

    @Autowired
    private LifeMapper lifeMapper;

    @Override
    public void run(String... args) {
        try {
            log.info("初始化人生模拟器数据库表...");
            lifeMapper.createLifeUserTable();
            lifeMapper.createLlmConfigTable();
            lifeMapper.createLifeCharacterTable();
            lifeMapper.createLifeEventTable();

            // 迁移：为已存在的 life_llm_config 表添加 custom_prompt 列
            try {
                lifeMapper.addCustomPromptColumn();
            } catch (Exception ignored) {
                // 列已存在 → 忽略
            }
            log.info("人生模拟器数据库表初始化完成");
        } catch (Exception e) {
            log.warn("人生模拟器数据库表初始化失败（可能已存在或数据库未就绪）: {}", e.getMessage());
        }
    }
}
