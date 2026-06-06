# 星尘观测站

一个以前端单页博客为主体、后端提供内容管理与 Agent 对话能力的个人站点项目。当前代码包含博客主页、BBS、画廊、音乐页、Live2D 游戏角，以及基于 `AiCodeHelper` 的 SSE 流式对话能力。

## 技术栈

### 前端

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Live2D Cubism SDK for Web 5

### 后端

- Java 17
- Spring Boot 3.5.13
- MyBatis
- MySQL
- Spring Security Crypto
- LangChain4j
- LangChain4j OpenAI Starter
- LangChain4j MCP
- Reactor

## 目录结构

以下只列出当前仓库中已经提交到 Git、且与现有实现直接相关的目录和关键文件：

```text
personal-blog/
├─ .gitignore
├─ README.md
├─ backend/
│  ├─ pom.xml
│  ├─ mvnw
│  ├─ mvnw.cmd
│  ├─ .mvn/
│  │  └─ wrapper/
│  ├─ src/
│  │  ├─ main/
│  │  │  ├─ java/com/azhi/
│  │  │  │  ├─ PersonalBlogApplication.java
│  │  │  │  ├─ config/
│  │  │  │  ├─ controller/
│  │  │  │  ├─ mapper/
│  │  │  │  ├─ pojo/
│  │  │  │  ├─ service/
│  │  │  │  └─ service/impl/
│  │  │  └─ resources/
│  │  │     ├─ application.yml
│  │  │     ├─ application-local.yml
│  │  │     └─ system-prompt.txt
│  │  └─ test/java/com/azhi/
│  ├─ code.txt
│  └─ post.txt
├─ frontend/
│  ├─ package.json
│  ├─ package-lock.json
│  ├─ vite.config.js
│  ├─ index.html
│  ├─ .env.example
│  ├─ public/
│  │  ├─ Core/
│  │  ├─ Framework/Shaders/WebGL/
│  │  ├─ Resources/
│  │  │  └─ Yachiyo/
│  │  ├─ image/
│  │  └─ favicon.ico
│  └─ src/
│     ├─ App.vue
│     ├─ main.js
│     ├─ api/
│     ├─ assets/images/
│     ├─ router/
│     └─ stores/
├─ Live2d/
│  ├─ CubismSdkForWeb-5-r.5/
│  └─ Yachiyo/
└─ agent/
   └─ agent/
      ├─ README.md
      ├─ backend-node-reference/
      ├─ backend-springboot/
      └─ frontend-vue3/
```

说明：

- `frontend/` 和 `backend/` 是当前实际运行代码。
- `Live2d/` 保存原始 SDK 与模型资源，`frontend/public/` 保存前端运行时实际访问的 Live2D 资源副本。
- `agent/` 目录仍在仓库中，但当前运行链路已经不再使用其中的 `room-agent` 参考实现。

## Live2D SDK 部署与动作设置

### 部署方式

当前前端并没有把 Live2D 单独拆成组件，而是直接在 [frontend/src/App.vue](/D:/personal-blog/frontend/src/App.vue) 中完成加载与交互。

运行时依赖的资源位于：

- `frontend/public/Core/live2dcubismcore.js`
- `frontend/public/Framework/Shaders/WebGL/*`
- `frontend/public/Resources/Yachiyo/*`
- `frontend/public/Resources/back_class_normal.png`
- `frontend/public/Resources/icon_gear.png`

源码构建阶段依赖的 SDK 与 Demo 源码位于：

- `Live2d/CubismSdkForWeb-5-r.5/Framework/src`
- `Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src`

对应的 Vite 别名配置在 [frontend/vite.config.js](/D:/personal-blog/frontend/vite.config.js)：

- `@framework` -> `../Live2d/CubismSdkForWeb-5-r.5/Framework/src`
- `@live2d-demo` -> `../Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src`

同时开启了：

- `server.fs.allow = ['..']`

这样开发环境才能读取 `frontend/` 外部的 SDK 源码目录。

### 页面挂载逻辑

当前游戏角页面使用 `currentPage === 'games'` 作为显示条件，并在页面切换时动态挂载、销毁 Live2D 运行时。核心点如下：

- 进入游戏角后先等待 `nextTick()`，再执行挂载逻辑。
- 运行前会动态注入 `/Core/live2dcubismcore.js`。
- 页面离开游戏角时会停止渲染循环并解绑指针事件。
- `watch(currentPage)` 和 `watch(live2dCanvas)` 两层逻辑共同保证画布可用时再初始化。

### 当前模型与动作设置

当前固定使用 `Yachiyo` 模型，并通过代码覆盖 SDK Demo 的模型目录：

- `live2dDefine.ModelDir` 只保留 `Yachiyo`

当前交互行为：

- 鼠标移动时角色会跟随指针方向。
- 点击角色时会触发随机表情。
- 页面切换离开游戏角时销毁当前实例，回到游戏角后重新初始化。

当前模型 JSON 为：

- `frontend/public/Resources/Yachiyo/Yachiyo.model3.json`

如果模型文件名包含中文，当前项目使用 JSON 中的 Unicode 转义写法来避免路径乱码问题。后续替换模型时，优先检查模型 JSON 中的文件名是否与真实文件完全一致。

## 用户认证逻辑

当前站点的登录态是 **Session 认证**，不是 JWT 认证。

### 登录与注册

后端接口位于 [backend/src/main/java/com/azhi/controller/UserController.java](/D:/personal-blog/backend/src/main/java/com/azhi/controller/UserController.java)：

- `POST /users/register`
- `POST /users/login`
- `POST /users/logout`

注册或登录成功后，后端都会把用户对象写入 `HttpSession`：

- Session Key：`currentUser`

### 前端请求行为

前端请求实例位于 [frontend/src/api/request.js](/D:/personal-blog/frontend/src/api/request.js)：

- `withCredentials: true`
- 默认会携带 Cookie，请求后端 Session

文件里仍保留了从 `localStorage` 读取 `token` 并写入 `Authorization` 头的兼容逻辑，但当前后端认证链路并不依赖这个 token。实际生效的登录态仍然是 `HttpSession`。

### 权限判断

帖子发布接口位于 [backend/src/main/java/com/azhi/controller/PostController.java](/D:/personal-blog/backend/src/main/java/com/azhi/controller/PostController.java)：

- 发帖前调用 `requireLogin(session)`，未登录会直接拒绝。
- 管理员判断方式是用户名等于 `AZHI4514`。
- 删除帖子时，管理员可直接删除，普通用户需要提供 `deleteKey`。

## Agent 配置与实现逻辑

### 当前接入方式

当前游戏角的 Agent 对话功能已经移除旧的 `room-agent` 方案，统一改为前端直接请求后端 `/ai/chat`。

后端入口位于 [backend/src/main/java/com/azhi/controller/AiControlller.java](/D:/personal-blog/backend/src/main/java/com/azhi/controller/AiControlller.java)：

- `GET /ai/chat`
- 返回类型：`text/event-stream`
- 请求参数：
  - `memoryId`
  - `message`

返回的 SSE 事件包括：

- `message`：流式文本分片
- `done`：结束事件
- `error`：错误事件

前端在 [frontend/src/App.vue](/D:/personal-blog/frontend/src/App.vue) 中通过 `EventSource('/ai/chat?...')` 建立连接，并实时把返回内容追加到聊天面板。

### AiCodeHelper 实现

核心 Service 接口位于：

- [backend/src/main/java/com/azhi/service/AiCodeHelperService.java](/D:/personal-blog/backend/src/main/java/com/azhi/service/AiCodeHelperService.java)

具体装配位于：

- [backend/src/main/java/com/azhi/service/impl/AiCodeHelperServiceImpl.java](/D:/personal-blog/backend/src/main/java/com/azhi/service/impl/AiCodeHelperServiceImpl.java)

当前实现逻辑：

- 使用 `AiServices.builder(...)` 构建 AI 服务。
- 同时注入普通 `ChatModel` 和 `StreamingChatModel`。
- 使用 `MessageWindowChatMemory.withMaxMessages(10)` 作为窗口记忆。
- 通过 `chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))` 为不同对话 ID 分配独立短期记忆。
- 挂载 `mcpToolProvider`，让模型可以调用 MCP 工具。
- 系统提示词来自 `backend/src/main/resources/system-prompt.txt`。
- 输入保护使用 `SafeInputGuardrail`。

### 模型与 MCP 配置

后端模型配置位于：

- [backend/src/main/resources/application.yml](/D:/personal-blog/backend/src/main/resources/application.yml)
- [backend/src/main/resources/application-local.yml](/D:/personal-blog/backend/src/main/resources/application-local.yml)

当前默认配置要点：

- 对话模型：`mimo-v2.5`
- 流式对话模型：`mimo-v2.5`
- 模型服务地址通过 `MIMO_BASE_URL` 配置
- 模型密钥通过 `MIMO_API_KEY` 配置
- DashScope 密钥通过 `DASHSCOPE_API_KEY` 配置

MCP 配置位于 [backend/src/main/java/com/azhi/config/McpConfig.java](/D:/personal-blog/backend/src/main/java/com/azhi/config/McpConfig.java)。

当前固定接入：

- MCP 服务地址：`https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/mcp`
- 鉴权方式：`Authorization: Bearer ${DASHSCOPE_API_KEY}`

游戏角前端展示的配置信息是固定文案，当前页面显示为：

- 使用模型：`Mimo-v2.5`
- 是否启用长期记忆：`是`
- 是否启用 MCP 服务：`是`
- MCP 服务模型：`Dashscope`
- 调用 MCP 服务工具：`web_search`

说明：

- 这里的“长期记忆”并不是数据库持久化记忆，而是当前 `memoryId` 维度下的会话窗口记忆。
- 当前代码中没有 `room_agent_config` 表，也没有对应 mapper。
- 当前 Agent 对话功能不包含图片上传接口。

## 服务器部署

以下部署说明基于当前代码结构，采用 `Nginx + Spring Boot Jar + MySQL` 的常见部署方式。

### 1. 环境要求

- Node.js 20 及以上
- Java 17
- Maven 3.9 及以上
- MySQL 8
- Nginx

### 2. 后端环境变量

生产环境至少需要配置以下变量：

```bash
PORT=8080
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=blog_db
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
UPLOAD_PATH=/opt/personal-blog/uploads
CORS_ALLOWED_ORIGINS=https://your-domain.com
MIMO_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
MIMO_API_KEY=your_mimo_api_key
DASHSCOPE_API_KEY=your_dashscope_api_key
```

说明：

- `application-local.yml` 中带有本地默认值，适合本机开发。
- `application.yml` 面向环境变量部署。
- 生产环境不要直接使用仓库中的示例密钥。

### 3. 本地开发启动

前端：

```bash
cd frontend
npm install
npm run dev
```

后端：

```bash
cd backend
mvn spring-boot:run
```

默认情况下：

- 前端开发服务运行在 `5173`
- 后端运行在 `8080`
- Vite 会把 `/clap`、`/images`、`/uploads`、`/musics`、`/posts`、`/ai`、`/users`、`/visitor-stats` 代理到后端

### 4. 前端构建

```bash
cd frontend
npm install
npm run build
```

构建产物位于：

- `frontend/dist`

### 5. 后端打包

```bash
cd backend
mvn clean package -DskipTests
```

Jar 包默认输出到：

- `backend/target/personal-blog-0.0.1-SNAPSHOT.jar`

### 6. Nginx 反向代理建议

前端静态文件可部署到 Nginx 站点目录，例如：

- `/var/www/personal-blog`

后端 Jar 可运行在：

- `127.0.0.1:8080`

Nginx 需要至少代理这些路径到后端：

- `/posts`
- `/images`
- `/uploads`
- `/musics`
- `/clap`
- `/users`
- `/visitor-stats`
- `/ai`

如果使用正则 location，不要写成 `location ~ ^/(posts|images|uploads|musics|clap|users|visitor-stats|ai)/`。
这个写法只会匹配 `/posts/123` 这类带下一级路径的请求，不会匹配 `GET /posts`、`GET /images`、`GET /musics`。
至少应改成 `location ~ ^/(posts|images|uploads|musics|clap|users|visitor-stats|ai)(/|$)`，或者直接分别使用前缀匹配。

其中 `/ai/chat` 是 SSE 接口，反向代理时不要把它改成只允许 `POST`，否则会出现 `HTTP 405`。

### 7. 上传目录

后端会通过 [backend/src/main/java/com/azhi/config/WebConfig.java](/D:/personal-blog/backend/src/main/java/com/azhi/config/WebConfig.java) 将：

- `/uploads/**`

映射到本地上传目录 `file.upload.path`。部署时需要提前创建这个目录，并保证后端进程有读写权限。
