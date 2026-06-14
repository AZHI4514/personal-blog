-- 人生模拟器 数据库初始化脚本
-- 数据库: blog_db
-- 执行方式: mysql -u root -p blog_db < life_simulator_init.sql

-- 用户设备绑定表
CREATE TABLE IF NOT EXISTS life_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_id VARCHAR(128) NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 大模型配置表（api_key 加密存储）
CREATE TABLE IF NOT EXISTS life_llm_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  base_url VARCHAR(255) NOT NULL,
  api_key VARCHAR(512) NOT NULL,
  model_name VARCHAR(64) DEFAULT 'gpt-3.5-turbo',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES life_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 角色存档表
CREATE TABLE IF NOT EXISTS life_character (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  age INT DEFAULT 0,
  money INT DEFAULT 100,
  health INT DEFAULT 80,
  happiness INT DEFAULT 60,
  morality INT DEFAULT 50,
  knowledge INT DEFAULT 30,
  is_alive BOOLEAN DEFAULT TRUE,
  generation INT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES life_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 事件历史表
CREATE TABLE IF NOT EXISTS life_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  character_id BIGINT NOT NULL,
  age INT NOT NULL,
  description TEXT NOT NULL,
  choice_made VARCHAR(255),
  effects JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (character_id) REFERENCES life_character(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
