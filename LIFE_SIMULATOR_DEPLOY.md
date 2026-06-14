# 人生模拟器 部署文档

## 环境要求

| 项目 | 版本要求 | 当前环境 |
|------|----------|----------|
| Node.js | ≥ 20 | v22.17.1 ✓ |
| JDK | 17 | 17.0.12 (C:\Program Files\Java\jdk-17) |
| MySQL | ≥ 8.0 | 9.6.0 (D:\Develop\mysql-9.6.0-winx64) |
| Maven | 3.9+ | 3.9.14 (通过 mvnw wrapper) |

## 项目结构

```
D:\personal-blog\
├── LOOP_PRINCIPLES.md                    ← 开发原则与进度追踪
├── LIFE_SIMULATOR_DEPLOY.md             ← 本文档
├── frontend/
│   └── src/
│       ├── api/life.js                  ← 人生模拟器 API 模块
│       ├── components/
│       │   ├── LifeSimulator.vue        ← 游戏主界面组件
│       │   └── LlmConfigPanel.vue      ← LLM 配置面板组件
│       ├── pages/GamesPage.vue          ← 已更新的游戏角页面
│       ├── composables/useBlogApp.js    ← 未修改（复用现有 gameActivePanel）
│       └── utils/storage.js            ← 未修改（复用 localStorage 工具）
└── backend/
    └── src/main/java/com/azhi/
        ├── config/
        │   ├── CryptoConfig.java        ← 加密配置（AES-256-CBC）
        │   └── LifeTableInitializer.java ← 数据库表自动初始化
        ├── controller/
        │   └── LifeController.java      ← 8 个 REST 端点
        ├── service/
        │   ├── DynamicLLMService.java   ← 动态 LLM 模型创建与调用
        │   ├── LifeService.java         ← 游戏逻辑接口
        │   ├── LlmConfigService.java    ← LLM 配置管理接口
        │   └── impl/
        │       ├── LifeServiceImpl.java ← 游戏逻辑实现
        │       └── LlmConfigServiceImpl.java ← 配置管理实现
        ├── mapper/
        │   └── LifeMapper.java          ← MyBatis Mapper（含 DDL）
        └── pojo/
            ├── LifeUser.java
            ├── LlmConfig.java
            ├── LifeCharacter.java
            ├── LifeEvent.java
            └── LifeActionRequest.java
```

## 数据库初始化

### 方式一：自动初始化（推荐）
后端启动时 `LifeTableInitializer` 会自动执行 CREATE TABLE IF NOT EXISTS，无需手动操作。

### 方式二：手动执行 SQL
```bash
mysql -u root -p blog_db < backend/src/main/resources/life_simulator_init.sql
```

### 新增数据表
| 表名 | 说明 | 字段 |
|------|------|------|
| `life_user` | 用户设备绑定 | id, device_id, created_at |
| `life_llm_config` | LLM 配置（api_key 加密） | id, user_id, base_url, api_key, model_name, created_at, updated_at |
| `life_character` | 角色存档 | id, user_id, name, age, money, health, happiness, morality, knowledge, is_alive, generation, created_at, updated_at |
| `life_event` | 事件历史 | id, character_id, age, description, choice_made, effects(JSON), created_at |

## 启动命令

### 1. 启动 MySQL
```powershell
Start-Service -Name "MySQL"
# 或
net start MySQL
```

### 2. 启动后端（端口 8080）
```powershell
cd D:\personal-blog\backend
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

### 3. 启动前端开发服务器（端口 5173）
```powershell
cd D:\personal-blog\frontend
npm run dev
```

### 4. 访问游戏
浏览器打开 `http://localhost:5173` → 导航栏点击「游戏角」→ 顶部工具栏点击「🎮 人生模拟器」

## API 端点速查

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/life/llm/config` | 保存/更新 LLM 配置 |
| GET | `/api/life/llm/config?deviceId=xxx` | 获取脱敏 LLM 配置 |
| POST | `/api/life/llm/test` | 测试 LLM 连接 |
| POST | `/api/life/start` | 初始化新角色 |
| POST | `/api/life/action` | 提交用户选择 |
| GET | `/api/life/state?characterId=xxx` | 获取角色状态 |
| GET | `/api/life/events?characterId=xxx&page=1&size=20` | 获取事件历史 |
| POST | `/api/life/reset` | 重新开局 |

所有 API 返回格式：`{ code: 200, data: {...}, message: "ok" }`

## 用户使用流程

1. 用户首次进入「人生模拟器」→ 看到 LLM 配置面板
2. 填写 base_url、api_key、model_name（可选）
3. 点击「测试连接」验证配置
4. 测试通过后点击「保存并开始」
5. 输入角色名字 → 点击「开始游戏」
6. AI 生成第一段剧情 + 选项
7. 用户选择 → AI 生成下一段剧情 + 新选项
8. 属性实时变化显示
9. 角色死亡 → 点击「重新开局」继承部分成就
10. 关闭页面后重新打开 → 自动恢复游戏进度

## 安全说明

### API Key 加密存储
- 用户输入的 `api_key` 在存储前使用 **Spring Security Crypto** 的 `TextEncryptor` 进行 AES-256-CBC 加密
- 加密密钥通过 `app.crypto.password` 配置（默认值：`personal-blog-life-sim-secret`）
- 解密仅在内存中进行，数据库存储的是密文
- 前端调用 GET `/api/life/llm/config` 时返回脱敏数据（仅显示前 4 后 4 字符）
- API Key 不会在前端 console、URL 参数或错误日志中出现

### 生产环境建议
1. 将 `app.crypto.password` 和 `app.crypto.salt` 配置为强随机值
2. 通过环境变量注入加密参数，不要硬编码在配置文件中
3. 考虑使用 HashiCorp Vault 或 AWS Secrets Manager 管理密钥
4. 启用 HTTPS，确保 API Key 在传输过程中加密

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `PORT` | 后端端口 | 8080 |
| `DB_HOST` | MySQL 主机 | 127.0.0.1 |
| `DB_PORT` | MySQL 端口 | 3306 |
| `DB_NAME` | 数据库名 | blog_db |
| `DB_USERNAME` | 数据库用户 | root |
| `DB_PASSWORD` | 数据库密码 | 1234 |
| `CORS_ALLOWED_ORIGINS` | CORS 允许的前端域名 | http://localhost:5173,http://localhost:5174 |

## 常见问题

### Q: MySQL 连接失败
A: 确保 MySQL 服务已启动。如果远程 MySQL (192.168.88.130) 不可达，可修改 `application-local.yml` 中的 `DB_HOST` 为 `127.0.0.1`，或启动本地 MySQL。

### Q: AI 生成超时
A: LLM 调用设置了 3 秒超时。检查 base_url 是否可达、api_key 是否有效。可以在 `DynamicLLMService.java` 中调整 `TIMEOUT` 常量。

### Q: 游戏进度丢失
A: 角色 ID 保存在 localStorage 中（键名 `lifeSim:<deviceId>:characterId`）。清除浏览器数据会导致进度丢失，但后端数据库中的角色数据仍在，联系管理员可恢复。
