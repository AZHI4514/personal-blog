# Agent Room Template

目标技术栈是 `Vue 3 + Spring Boot`：

1. `Live2D 私人居所`
2. `Room Agent 化能力`
3. `角色知识库管理`

模板同时提供两套参考：

- `frontend-vue3/`：可直接迁到 Vue3 博客前端
- `backend-springboot/`：适配 Spring Boot 的接口骨架
- `backend-node-reference/`：从当前项目抽出来的原始实现思路，方便对照

## 功能拆解

### 1. Live2D 私人居所

包含这些子能力：

- 天气 / 时间驱动房间氛围
- 浏览器侧 LLM 聊天
- TTS 语音播放
- 音乐播放卡片

对应模板文件：

- `frontend-vue3/src/composables/useAgentRoomWorld.js`
- `frontend-vue3/src/composables/useAgentRoomChat.js`
- `frontend-vue3/src/composables/useAgentRoomMusic.js`
- `frontend-vue3/src/services/agentStorage.js`
- `backend-springboot/src/main/java/com/example/blog/agent/controller/RoomAgentController.java`

### 2. Room Agent 化能力

包含这些子能力：

- 长期记忆
- 用户独立记忆
- MCP 工具接入
- 图片理解兜底

对应模板文件：

- `frontend-vue3/src/composables/useAgentRoomChat.js`
- `backend-node-reference/services/room-memory.js`
- `backend-node-reference/services/mcp-stdio.js`
- `backend-springboot/src/main/java/com/example/blog/agent/service/RoomMemoryService.java`

### 3. 角色知识库可在房间设置页管理

目标是稳定还原“八千代”的人格、语气、行为边界。

对应模板文件：

- `frontend-vue3/src/constants/knowledgeEntries.js`
- `frontend-vue3/src/pages/RoomAgentSettings.vue`

## 推荐复刻方式

## 第一步：先把接口边界固定

建议 Spring Boot 先提供这几个接口：

- `GET /api/room-agent/world`
- `GET /api/room-agent/memory`
- `POST /api/room-agent/memory`
- `PATCH /api/room-agent/memory/{id}`
- `DELETE /api/room-agent/memory/{id}`
- `POST /api/room-agent/mcp/call`

其中：

- `world` 负责天气、时间段、季节、地点
- `memory` 负责每个用户独立的长期记忆
- `mcp/call` 负责后端代理 MCP 或安全白名单工具

## 第二步：前端先跑通本地存储

先接入：

- `roomLLMSettings`
- `roomTTSSettings`
- `roomKnowledgeSettings`
- `roomMemorySettings`
- `roomMCPSettings`

这一步不依赖后端，能先把 UI、知识库管理、浏览器直连 LLM/TTS 跑起来。

## 第三步：把聊天上下文拼装做好

`useAgentRoomChat.js` 里有一条核心链路：

1. 读角色知识库
2. 拉取当前用户长期记忆
3. 如果有图且配置了 `visionMode=mcp/auto`，优先调图片理解
4. 如果用户问题像“查一下/最新/官网”，可调用 MCP 搜索
5. 组合成最终 `system prompt + context`
6. 浏览器直接请求 LLM，或走后端代理


## 第四步：长期记忆先做“够用版”

模板里的长期记忆不是重型向量库方案，而是“轻量 embedding + 相似度检索”的够用版：

- 每条记忆属于一个用户
- 自动过滤敏感信息
- 自动判断是否值得长期保存
- 相似记忆合并
- 检索时按相似度 + 重要度 + 新鲜度排序


Spring Boot 里建议：

- 存 SQLite / PostgreSQL 都可以
- `embedding` 先存 JSON 数组
- 后续再换 pgvector / milvus 都不影响接口层

## 第五步：角色知识库不要和长期记忆混存

这两者职责不同：

- 角色知识库：稳定人格、口吻、边界、世界观
- 用户长期记忆：记录“这个用户是谁、偏好什么、之前聊过什么”

所以推荐分开：

- `knowledge_entries`：房间级、角色级配置
- `room_memories`：用户级记忆

模板里设置页也是按这个思路拆的。

## 第六步：MCP 接入要做安全边界

不要让浏览器随意调用任意 MCP。

推荐做法：

- 浏览器只存 MCP 配置
- 真正敏感工具放后端白名单代理
- 工具按 allowlist 控制
- 图片理解、网页搜索优先做成固定工具名

最小白名单：

- `understand_image`
- `web_search`

## 第七步：Live2D 与 Agent 解耦

不要把 Live2D 动作控制写死在模型回复文本里。

推荐格式：

```json
{
  "reply": "要显示给用户的文本",
  "live2d": {
    "emotion": "happy",
    "expression": "smile",
    "motion": "none",
    "intensity": 0.6,
    "durationMs": 5000
  }
}
```

这样以后换模型、换角色、换 TTS，都不影响 Live2D 控制层。

## 目录说明

- `frontend-vue3/`
  - Vue3 侧模板
- `backend-springboot/`
  - 适合博客后端落地的 Java 骨架
- `backend-node-reference/`
  - 当前项目中抽出来的参考实现

## 建议的复刻顺序

1. 先接 `RoomAgentSettings.vue` 和本地存储
2. 再接 `useAgentRoomWorld.js` 和天气卡片
3. 再接 `useAgentRoomMusic.js`
4. 再接 `useAgentRoomChat.js`，先只跑浏览器直连 LLM
5. 再补 `memory` 接口
6. 最后补 `MCP` 和图片理解兜底

## 真正要保留的设计原则

- 角色知识库和用户记忆分离
- 浏览器配置与服务端记忆分离
- LLM、TTS、MCP 都通过统一 settings 管理
- 房间氛围状态由 `weather + timePhase + season` 决定
- Agent 上下文构造要可插拔，不要写死在某一个模型 SDK 里

