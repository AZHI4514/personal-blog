# 星尘观测站

一个以前端单页博客为主体、后端提供内容管理与 AI 对话能力的个人站点项目。当前包含首页、个人资料、画廊、音乐页、BBS、以及 Live2D 游戏角。

## 目录

- [技术栈](#技术栈)
- [目录结构](#目录结构)
- [主要功能](#主要功能)
- [前端说明](#前端说明)
  - [当前结构与页面组织](#当前结构与页面组织)
  - [浏览器兼容](#浏览器兼容)
  - [启动期容错](#启动期容错)
  - [Live2D 自定义与问题排查](#live2d-自定义与问题排查)
- [用户认证](#用户认证)
- [AI 对话](#ai-对话)
- [人生模拟器](#人生模拟器)
- [本地开发](#本地开发)
- [构建](#构建)
- [生产部署](#生产部署)
- [后端环境变量](#后端环境变量)
- [上传目录](#上传目录)
- [统一响应格式](#统一响应格式)
- [业务模块](#业务模块)
  - [BBS 论坛](#bbs-论坛)
  - [画廊](#画廊)
  - [音乐播放器](#音乐播放器)
  - [鼓掌/点赞](#鼓掌点赞)
  - [访客统计](#访客统计)
  - [管理员后台](#管理员后台)
  - [链接与规则页](#链接与规则页)
- [LangChain4j 与 MCP 集成](#langchain4j-与-mcp-集成)
- [全局异常处理](#全局异常处理)
- [后端 API 接口汇总](#后端-api-接口汇总)
- [CORS 与静态资源配置](#cors-与静态资源配置)

## 技术栈

### 前端

- Vue 3
- Vite 7
- Vue Router
- Axios
- Live2D Cubism SDK for Web 5
- `@vitejs/plugin-vue`
- `@vitejs/plugin-legacy`

### 后端

- Java 17
- Spring Boot 3.5.13
- Spring Web MVC
- MyBatis
- MySQL 8
- Lombok
- Spring Security Crypto
- LangChain4j
- Reactor

## 目录结构

```text
personal-blog/
├─ frontend/                        # Vue 3 + Vite 前端工程
│  ├─ public/                       # 直接对外提供的静态资源与 Live2D 运行时资源
│  ├─ src/
│  │  ├─ api/                       # 前端接口封装
│  │  ├─ assets/                    # 构建期静态资源
│  │  ├─ components/               # 共享组件（LifeSimulator.vue / LlmConfigPanel.vue）
│  │  ├─ composables/               # 组合式业务逻辑
│  │  ├─ layouts/                   # 页面整体布局
│  │  ├─ pages/                     # 路由页面
│  │  ├─ router/                    # Vue Router 路由配置
│  │  ├─ styles/                    # 全局样式
│  │  ├─ utils/                     # 工具函数
│  │  ├─ App.vue                    # 应用入口组件
│  │  └─ main.js                    # 前端启动入口
│  ├─ index.html
│  ├─ package.json
│  └─ vite.config.js
├─ backend/                         # Spring Boot 后端工程
│  ├─ src/main/java/com/azhi/
│  │  ├─ config/                    # Web（CORS+静态资源）、MCP（联网搜索）、Crypto（AES 加解密）、LifeTableInitializer（自动建表）
│  │  ├─ controller/                # 控制器（LifeController / PostController / AiControlller / GalleryController / MusicController / UploadController 等）
│  │  ├─ mapper/                    # MyBatis Mapper（LifeMapper / PostMapper / ImageMapper / MusicMapper / ClapMapper 等）
│  │  ├─ pojo/                      # 实体与统一返回结构（User / Post / Image / Music / Result / LifeCharacter / LifeEvent 等）
│  │  ├─ service/                   # 接口（顶层）+ impl/（实现），含 LifeService / AiCodeHelperService / DynamicLLMService / SafeInputGuardrail 等
│  │  └─ PersonalBlogApplication.java
│  ├─ src/main/resources/           # application.yml、system-prompt.txt、life_simulator_init.sql
│  ├─ uploads/                      # 本地上传目录
│  └─ pom.xml
├─ Live2d/                          # Live2D 原始 SDK 与模型素材
├─ deploy/                          # 部署相关配置
├─ database.md                      # 数据库说明
├─ blog_db_backup.sql               # 数据库备份示例
└─ README.md
```

说明：

- `frontend/` 和 `backend/` 是当前实际运行的主项目代码。
- `frontend/src/pages/` 负责页面拆分，`frontend/src/layouts/DefaultLayout.vue` 负责整体骨架，`frontend/src/composables/useBlogApp.js` 集中管理主要前端状态与业务逻辑。
- `frontend/src/api/` 封装所有后端接口调用，模块按功能拆分（`post.js`、`user.js`、`music.js`、`gallery.js`、`life.js` 等），统一通过 `request.js` Axios 实例发起请求。
- `frontend/public/` 保存构建后仍需按原路径直接访问的资源，例如 Live2D 运行时文件、图片与音乐资源。
- `backend/src/main/java/com/azhi/service/` 下接口与 `impl/` 实现分离；`config/` 集中管理 Web/CORS、MCP 工具、AES 加解密和数据库自动建表。
- `backend/src/main/java/com/azhi/controller/GlobalExceptionHandler.java` 为全局异常拦截器，统一返回 `Result` 错误格式。
- `backend/uploads/` 用于保存后端上传的图片和音乐文件。
- `Live2d/` 保存原始 SDK 与模型素材，Vite 构建阶段会引用其中的源码。

## 主要功能

- 复古风个人主页与导航
- 个人资料页 / 100 问 100 答
- 画廊展示与后台上传
- 音乐列表、播放控制、封面展示
- BBS 发帖、回复、编辑、删除（deleteKey 机制 + 管理员权限）
- 鼓掌/点赞互动
- 访客统计（IP 去重）
- 基于 Session 的用户登录 / 注册
- Live2D 游戏角
- `/ai/chat` SSE 流式对话（LangChain4j + MCP 联网搜索 + 敏感词过滤）
- 人生模拟器（文字版 GTA）：用户自配 LLM 动态生成开放世界剧情
- 管理员后台（内容管理）
- 友情链接与站规展示

## 前端说明

### 当前结构与页面组织

当前前端采用 Vue Router 拆分页面，而不是把整站内容集中在单个组件里。核心组织方式如下：

- [frontend/src/main.js](/abs/path/D:/personal-blog/frontend/src/main.js:1) 负责创建应用、注册路由，并在启动失败时输出错误信息
- [frontend/src/App.vue](/abs/path/D:/personal-blog/frontend/src/App.vue:1) 只作为应用入口，直接挂载默认布局
- [frontend/src/layouts/DefaultLayout.vue](/abs/path/D:/personal-blog/frontend/src/layouts/DefaultLayout.vue:1) 负责顶部栏、侧边栏、页脚和 `RouterView`
- [frontend/src/pages/](/abs/path/D:/personal-blog/frontend/src/pages) 下的页面组件分别承载首页、资料页、画廊、BBS、规则页、游戏角、音乐页、管理员页、链接页和 404 页
- [frontend/src/composables/useBlogApp.js](/abs/path/D:/personal-blog/frontend/src/composables/useBlogApp.js:1) 集中管理登录状态、帖子、画廊、音乐、Live2D、AI 对话、启动期错误等前端状态与业务逻辑
- [frontend/src/styles/app.css](/abs/path/D:/personal-blog/frontend/src/styles/app.css:1) 负责全站样式

当前路由位于 [frontend/src/router/index.js](/abs/path/D:/personal-blog/frontend/src/router/index.js:1)，已接入的页面包括：

- `/`
- `/profile`
- `/gallery`
- `/bbs`
- `/rules`
- `/games`
- `/music`
- `/admin`
- `/links`
- `/:pathMatch(.*)*`

如果要阅读当前前端代码，比较合适的顺序是：

1. [frontend/src/router/index.js](/abs/path/D:/personal-blog/frontend/src/router/index.js:1)
2. [frontend/src/layouts/DefaultLayout.vue](/abs/path/D:/personal-blog/frontend/src/layouts/DefaultLayout.vue:1)
3. `frontend/src/pages/*.vue`
4. [frontend/src/composables/useBlogApp.js](/abs/path/D:/personal-blog/frontend/src/composables/useBlogApp.js:1)
5. `frontend/src/api/*.js`
6. [frontend/src/styles/app.css](/abs/path/D:/personal-blog/frontend/src/styles/app.css:1)

### 浏览器兼容

这次移动端渲染问题的典型表现是：

- 页面只显示纯色背景
- CSS 背景已经出来了，但主体内容完全没有挂载
- 不同手机浏览器表现不一致，桌面端和部分移动端正常，部分自带浏览器、老 WebView、部分微信内核异常

根因不是后端接口本身，而是前端脚本在部分移动浏览器里启动失败。此次实际排查出的原因主要有两层：

- 构建产物最初只有现代浏览器可用的模块脚本，旧版移动浏览器无法正确执行
- 页面模板中存在非法 HTML 属性写法，某些移动浏览器在渲染时直接抛出 `InvalidCharacterError`，导致 Vue 根组件无法挂载

为解决这类问题，前端当前已启用 `@vitejs/plugin-legacy`，构建后会同时生成：

- 现代浏览器的 `type="module"` 入口
- 旧浏览器的 `nomodule` + `legacy` 回退脚本

同时做了以下兼容与排障处理：

- 降低旧移动浏览器的启动门槛，补齐 `legacy` 构建与 polyfill
- 为 `localStorage` 访问增加安全封装，避免部分 WebView 因存储异常在首屏崩溃
- 为应用挂载增加错误兜底，移动端脚本失败时可直接在页面上显示报错
- 修复模板中的非法属性写法，避免浏览器在创建 DOM 属性时直接中断渲染

部署时必须注意：

- 不要只上传 `Core`、`Framework`、`Resources` 等资源目录
- 必须完整部署 `frontend/dist/` 的全部内容，包括 `index.html` 和 `assets/`
- 每次前端更新后都需要重新执行 `npm run build`，再把 `dist/.` 整体复制到站点目录

如果再次出现“只有背景没有内容”的情况，优先检查：

- 线上 `index.html` 是否已经更新到最新构建版本
- `assets/` 下的现代脚本、legacy 脚本和 CSS 是否都能直接访问
- 页面是否已经输出启动错误信息，便于进一步定位具体浏览器报错

### 启动期容错

前端目前额外做了两层保护：

- `localStorage` 读写经过安全封装，避免某些 WebView 因存储异常在首屏直接崩溃
- 应用挂载失败时会在页面上直接输出错误信息，便于远程排查移动端兼容问题

### Live2D 自定义与问题排查

#### 代码与资源位置

前端 Live2D 逻辑集中在 [frontend/src/composables/useBlogApp.js](/abs/path/D:/personal-blog/frontend/src/composables/useBlogApp.js:1)，运行时模型入口为 [frontend/public/Resources/Yachiyo/Yachiyo.model3.json](/abs/path/D:/personal-blog/frontend/public/Resources/Yachiyo/Yachiyo.model3.json:1)。

运行时依赖资源：

- `frontend/public/Core/live2dcubismcore.js`
- `frontend/public/Framework/Shaders/WebGL/*`
- `frontend/public/Resources/Yachiyo/*`
- `frontend/public/Resources/back_class_normal.png`
- `frontend/public/Resources/icon_gear.png`

构建阶段依赖的 SDK 源码路径：

- `Live2d/CubismSdkForWeb-5-r.5/Framework/src`
- `Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src`

Vite 别名配置位于 [frontend/vite.config.js](/abs/path/D:/personal-blog/frontend/vite.config.js:1)。

#### 已知问题

##### 问题一：移动端黑轮廓 / 贴图缺失

**现象**：模型动作和点击反馈正常，但部分手机浏览器只显示黑色轮廓，材质贴图没有正常显示。

**原因**：不是动作系统问题，而是贴图资源或移动端 WebGL 兼容性问题。常见原因包括运行时资源中文路径导致部分移动端加载失败，以及贴图尺寸超过旧手机或 WebView 的最大纹理尺寸限制。

**已做处理**：

- 运行时资源路径改为优先使用 ASCII 名称，避免部分移动端对中文路径兼容不稳定
- Live2D 运行时贴图尺寸从原始超大纹理降到更适合移动端的级别，降低因最大纹理尺寸不足而导致贴图发黑的概率

**排查清单**：如果再次出现黑轮廓、贴图缺失但动作仍正常的情况，按顺序检查：

1. `frontend/public/Resources/Yachiyo/Yachiyo.model3.json` 中引用的贴图路径是否真实存在
2. `frontend/public/Resources/Yachiyo/textures/` 下的 `texture_00.png` 和 `texture_01.png` 是否已经部署到线上
3. 线上是否重新执行了前端构建，并把整个 `frontend/dist/.` 完整复制到站点目录

##### 问题二：路由切换后模型消失

**现象**：

- 第一次进入 `/games`，Live2D 模型正常加载并渲染
- 通过导航切换到其他页面（如首页、BBS 等）后，再切回 `/games`
- Live2D 画布不再显示模型，可能出现 `startupErrors` 报错

**根因分析**：

问题出在 Vue Router 的组件生命周期与 Live2D WebGL 实例之间的数据不一致，分为三步：

1. **离开时未释放**：Vue Router 切换路由时卸载 `GamesPage.vue`，`<canvas>` 从 DOM 移除，浏览器自动销毁其 WebGL 上下文。但旧代码在 `currentPage` watcher 中只调用了 `stopLive2dRenderLoop()` 和 `detachLive2dPointerEvents()`，这两个函数仅暂停动画循环和指针事件，并未调用 `release()` 清理 `live2dSubdelegate` 持有的 WebGL 资源。

2. **进入时错误复用**：当再次进入 `/games` 时，`mountLive2d()` 检测到 `live2dSubdelegate !== null`，进入复用分支直接重启渲染循环。但 subdelegate 内部绑定的仍是上一个已销毁 canvas 的失效 WebGL 上下文。渲染循环调用 `subdelegate.update()` 时，内部 `onResize()` 尝试访问已失效的视图对象，抛出 `Cannot read properties of null` 错误，Live2D 无法渲染。

3. **销毁路径同样危险**：如果在复用分支中尝试调用 `destroyLive2dInstance()` → `release()`，同样会失败。因为 `LAppSubdelegate.release()` 内部调用 WebGL 对象的 `unobserve()` 方法时，WebGL 上下文已被浏览器自动清理、相关对象已变为 null，导致 `Cannot read properties of null (reading 'unobserve')` 的二次释放错误。

**解决思路**：采用"每次进入游戏角时完整重建"策略，三处改动均位于 [frontend/src/composables/useBlogApp.js](/abs/path/D:/personal-blog/frontend/src/composables/useBlogApp.js:1)：

1. **离开时彻底销毁**：`currentPage` watcher 中，将 `stopLive2dRenderLoop()` + `detachLive2dPointerEvents()` 替换为 `destroyLive2dInstance()`。由于 `currentPage.value` 在 `router.push()` 之前同步修改，此时组件尚未卸载、canvas 仍在 DOM 中、WebGL 上下文有效，`release()` 可以安全执行。

2. **进入时完整重建**：`mountLive2d()` 中，将子代理复用分支替换为先 `destroyLive2dInstance()` 再走完整初始化流程，确保每次进入 `/games` 都创建全新 Live2D 实例。

3. **release() 容错保护**：`destroyLive2dInstance()` 中对 `release()` 增加 try-catch，防止边缘情况（浏览器直接刷新、WebGL 上下文异常丢失等）导致未捕获异常。

这一策略牺牲了每次进入时的 SDK 和纹理重新初始化开销，但换来了稳定性和可维护性，是当前项目规模和 Live2D SDK 版本下最可靠的方案。

#### 自定义指南

**资源级修改**（替换模型入口、表情、物理参数或贴图）：

- 模型入口：`frontend/public/Resources/Yachiyo/Yachiyo.model3.json`
- 表情文件：`frontend/public/Resources/Yachiyo/*.exp3.json`
- 物理参数：`frontend/public/Resources/Yachiyo/*.physics3.json`
- 贴图资源：`frontend/public/Resources/Yachiyo/textures/*.png`

**代码级修改**（自定义动作、表情触发或加载行为），参考 [frontend/src/composables/useBlogApp.js](/abs/path/D:/personal-blog/frontend/src/composables/useBlogApp.js:1093) 附近的初始化代码：

- 表情触发逻辑：调整 `LAppLive2DManager.prototype.onTap`
- 模型目录：调整 `live2dDefine.ModelDir`
- 加载提示文案与行为：调整 `mountLive2d` 和 `live2dLoading`

#### 修改后的部署步骤

每次修改 Live2D 资源或前端逻辑后，必须重新执行：

```bash
cd /opt/personal-blog/frontend
npm ci
npm run build
sudo find /var/www/personal-blog -mindepth 1 -maxdepth 1 -exec rm -rf {} +
sudo cp -a /opt/personal-blog/frontend/dist/. /var/www/personal-blog/
sudo nginx -t
sudo systemctl reload nginx
```

## 用户认证

当前项目使用的是 **浏览器 Cookie + 后端 HttpSession** 的认证方式。登录状态不由前端单独保存和解释，而是由后端在 Session 中维护，浏览器负责在后续请求里自动携带对应的 Cookie。

### 1. 前端如何发起认证请求

前端认证相关接口位于 [frontend/src/api/user.js](/abs/path/D:/personal-blog/frontend/src/api/user.js:1)，对应 3 个基础操作：

- `POST /users/register`
- `POST /users/login`
- `POST /users/logout`

这些请求统一通过 [frontend/src/api/request.js](/abs/path/D:/personal-blog/frontend/src/api/request.js:1) 中的 Axios 实例发送。这里的关键配置是：

- `withCredentials: true`

它的含义是：

- 注册或登录成功后，浏览器接收后端返回的 Session Cookie
- 之后访问同源接口时，浏览器会自动携带这个 Cookie
- 前端不需要自行拼接认证头，也不需要自己解析登录态

因此，前端的职责主要是提交用户名、密码等数据，并保持请求具备携带 Cookie 的能力。

### 2. 后端如何建立和清理登录态

用户认证入口位于 [backend/src/main/java/com/azhi/controller/UserController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/UserController.java:1)。

注册流程：

- 调用 `POST /users/register`
- 后端执行 `userService.register(user)` 创建用户
- 注册成功后执行 `session.setAttribute("currentUser", registeredUser)`

登录流程：

- 调用 `POST /users/login`
- 后端执行 `userService.login(username, password)` 校验账号密码
- 登录成功后执行 `session.setAttribute("currentUser", user)`

退出流程：

- 调用 `POST /users/logout`
- 后端执行 `session.invalidate()`
- 当前 Session 被销毁，登录态随之失效

这套机制的核心在于：后端把当前登录用户写入 `HttpSession`，属性名为 `currentUser`。只要浏览器后续请求仍然携带有效 Session Cookie，后端就能识别当前用户身份。

### 3. 密码如何处理

密码校验逻辑位于 [backend/src/main/java/com/azhi/service/impl/UserServiceImpl.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/impl/UserServiceImpl.java:1)。

- 注册时使用 `BCryptPasswordEncoder` 对密码做哈希后再保存
- 登录时先按用户名查询用户，再通过 `passwordEncoder.matches(...)` 校验输入密码
- 用户信息返回前会执行 `user.setPassword(null)`，避免把密码哈希返回给前端

因此，数据库中保存的是经过 BCrypt 处理后的密码，不是明文密码。

### 4. 受保护接口如何判断是否已登录

项目中的受保护接口会直接从 `HttpSession` 中读取当前用户，而不是依赖前端自报状态。以发帖接口为例，相关代码位于 [backend/src/main/java/com/azhi/controller/PostController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/PostController.java:1)。

- 创建帖子接口为 `POST /posts`
- 控制器方法直接接收 `HttpSession session`
- 进入业务逻辑前先执行登录校验

判断方式本质上就是检查：

- `session.getAttribute("currentUser")` 是否存在

如果这个值为空，后端就认为当前请求未登录，并拒绝继续执行需要身份的操作。

### 5. 前后端在认证链路中的分工

- 前端负责收集并提交认证信息，通过 `withCredentials: true` 让浏览器自动携带 Session Cookie
- 后端负责校验密码、创建和销毁 Session、写入 `currentUser`，并在需要权限的接口中基于 Session 判断用户身份

从整体上看，这是一套以服务端 Session 为中心的认证流程：登录态存放在后端，浏览器负责携带凭证，前端只负责调用接口和展示结果。

## AI 对话

当前 AI 对话基于 LangChain4j + Spring WebFlux 实现 SSE 流式输出。

### 请求方式

- 端点：`GET /ai/chat`
- 响应类型：`text/event-stream`
- 请求参数：`memoryId`（会话隔离标识）、`message`（用户消息）

### 核心文件

- 控制器：[AiControlller.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/AiControlller.java:1) — SSE 端点
- 接口定义：[AiCodeHelperService.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/AiCodeHelperService.java:1) — 声明式 AI Service
- 服务构建：[AiCodeHelperServiceImpl.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/impl/AiCodeHelperServiceImpl.java:1) — AiServices 装配
- 安全护栏：[SafeInputGuardrail.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/impl/SafeInputGuardrail.java:1) — 敏感词过滤
- MCP 配置：[McpConfig.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/config/McpConfig.java:1) — 联网搜索工具
- 系统提示词：[system-prompt.txt](/abs/path/D:/personal-blog/backend/src/main/resources/system-prompt.txt:1)

### 架构概览

| 层 | 组件 | 说明 |
|----|------|------|
| 对话模型 | `ChatModel` / `StreamingChatModel` | LangChain4j 自动注入，指向 `mimo-v2.5` |
| 会话记忆 | `MessageWindowChatMemory` | 每个 `memoryId` 保留最近 10 条消息 |
| 工具调用 | `McpToolProvider` | 阿里云 DashScope MCP Web 搜索 |
| 安全护栏 | `SafeInputGuardrail` | 模型调用前过滤敏感词 |
| 流式输出 | `Flux<ServerSentEvent<String>>` | Reactor 流式推送，事件类型 `message`/`done`/`error` |

详细的 LangChain4j 集成、MCP 配置、安全护栏和系统提示词说明见 [LangChain4j 与 MCP 集成](#langchain4j-与-mcp-集成)。

## 人生模拟器

人生模拟器是一个**文字版 GTA** 风格的游戏模块，集成在游戏角 `/games` 页面中。核心特点是：用户自己提供大模型（LLM）接口，后端动态调用 AI 生成开放式剧情。

### 概述

- 玩家配置自己的 LLM（支持任意 OpenAI 兼容 API），后端不预设模型
- 每个选择由 AI 实时计算属性变化（金钱、健康、快乐、道德、知识）并生成下一段剧情
- 属性降到 0 时角色死亡，游戏结束
- 支持断点续玩、事件历史回溯、存档删除
- 用户可自定义世界观、角色类型、禁用词等，AI 生成剧情时遵循自定义规则

### 页面结构

游戏入口位于游戏角的"🎮 人生模拟器"标签页，内部有两个子面板：

| 子面板 | 说明 |
|--------|------|
| **配置面板**（`LlmConfigPanel.vue`） | 输入 Base URL + API Key + 模型名称，测试连接，保存配置；设置自定义剧情风格（世界观/角色/禁用词）；开始新游戏或继续存档 |
| **游戏界面**（`LifeSimulator.vue`） | 展示角色属性条（金钱/健康/快乐/道德/知识）、当前剧情文本、2-4 个选项按钮；可查看事件记录、返回配置页、重新开始 |

相关文件：

- 前端页面入口：[frontend/src/pages/GamesPage.vue](/abs/path/D:/personal-blog/frontend/src/pages/GamesPage.vue:1) —— 管理 `lifeSimSubPanel` 切换逻辑
- 配置面板：[frontend/src/components/LlmConfigPanel.vue](/abs/path/D:/personal-blog/frontend/src/components/LlmConfigPanel.vue:1)
- 游戏界面：[frontend/src/components/LifeSimulator.vue](/abs/path/D:/personal-blog/frontend/src/components/LifeSimulator.vue:1)
- API 封装：[frontend/src/api/life.js](/abs/path/D:/personal-blog/frontend/src/api/life.js:1)
- 本地存储：[frontend/src/utils/storage.js](/abs/path/D:/personal-blog/frontend/src/utils/storage.js:1) —— 提供 `readJson` / `writeJson` 安全封装

### API 端点

所有端点位于 `/api/life/*`，由 [backend/src/main/java/com/azhi/controller/LifeController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/LifeController.java:1) 统一处理。

| 方法 | 端点 | 说明 |
|------|------|------|
| `POST` | `/api/life/llm/config` | 保存/更新 LLM 配置（apiKey 加密存储） |
| `GET` | `/api/life/llm/config` | 获取 LLM 配置（apiKey 脱敏） |
| `POST` | `/api/life/llm/test` | 测试 LLM 连接是否可用 |
| `POST` | `/api/life/start` | 初始化新角色并返回首段剧情 |
| `POST` | `/api/life/action` | 提交用户选择，返回下一段剧情 |
| `GET` | `/api/life/state` | 获取角色当前状态 |
| `GET` | `/api/life/events` | 获取事件历史（分页） |
| `DELETE` | `/api/life/character` | 删除指定角色及关联事件 |
| `DELETE` | `/api/life/user/data` | 删除用户全部数据（配置 + 角色 + 事件） |

### 后端架构

#### 核心服务

| 类 | 职责 |
|----|------|
| `LifeController` | 统一处理 `/api/life/*` 请求 |
| `LifeService` / `LifeServiceImpl` | 游戏核心逻辑：开局、回合推进、属性结算、AI 响应解析 |
| `LlmConfigService` / `LlmConfigServiceImpl` | LLM 配置的 CRUD、apiKey AES 加解密 |
| `DynamicLLMService` | 使用 Java 内置 `HttpClient` 直接调用 OpenAI 兼容 API，超时 3 秒 |

关键实现文件：

- [LifeController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/LifeController.java:1)
- [LifeService.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/LifeService.java:1)
- [LifeServiceImpl.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/impl/LifeServiceImpl.java:1)
- [DynamicLLMService.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/DynamicLLMService.java:1)
- [LifeMapper.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/mapper/LifeMapper.java:1)

#### 动态 LLM 调用的设计考量

项目已有 LangChain4j 的 `OpenAiChatModel`，但人生模拟器不使用它，原因是：

- LangChain4j 的 `OpenAiChatModel.builder()` 依赖 `ServiceLoader` 动态注册 HTTP 客户端
- 在同一个 JVM 中多次动态创建时会抛出 `Multiple HTTP client implementations found` 异常
- `DynamicLLMService` 使用 Java 内置 `HttpClient` 直接发送 OpenAI 兼容的 `/chat/completions` 请求，避免了 HTTP 客户端冲突

#### 属性变化机制

每回合 AI 返回 JSON 包含 `statChanges` 对象，每个属性变化范围 -20 到 +20：

- `money`（金钱）：工作、交易、投资等事件影响
- `health`（健康）：仅身体受伤、疾病、战斗等直接伤害事件可扣除；日常剧情只能保持或增加
- `happiness`（快乐）：社交、成功、失败等情绪事件影响
- `morality`（道德）：善恶抉择影响
- `knowledge`（知识）：学习、探索、与人交流影响

任何属性降至 0 或以下 → 角色死亡，游戏结束。

### 数据库

人生模拟器使用 4 张独立表（前缀 `life_`），与博客主表共享同一 `blog_db` 数据库。

```text
life_user          # 用户设备绑定（device_id → user_id）
life_llm_config    # LLM 配置（api_key AES 加密存储，custom_prompt 可自定义剧情风格）
life_character     # 角色存档（5 维属性 + 存活状态 + 世代数）
life_event         # 事件历史（age/description/choice_made/effects JSON）
```

DDL 脚本参考：

- [life_simulator_init.sql](/abs/path/D:/personal-blog/backend/src/main/resources/life_simulator_init.sql:1) —— 手动初始化
- [LifeTableInitializer.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/config/LifeTableInitializer.java:1) —— 应用启动时自动建表（`IF NOT EXISTS`，幂等安全）
- [LifeMapper.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/mapper/LifeMapper.java:1) —— DDL 通过 MyBatis `@Update` 注解执行

### 用户数据流

```
用户浏览器                        后端
    │                              │
    ├─1.填写 LLM 配置──────────────→│ AES 加密 apiKey → life_llm_config
    ├─2.测试连接──────────────────→│ 调用 /chat/completions，返回测试结果
    ├─3.开始游戏──────────────────→│ 创建 life_character + 调用 AI 生成开局剧情
    │←─返回首段剧情+选项──────────│
    ├─4.选择选项──────────────────→│ 调用 AI 计算属性变化+生成下一段剧情
    │←─返回新剧情+属性变化+新选项──│ 更新 life_character + 写入 life_event
    │   ...重复 4...               │
    ├─5.游戏结束/重新开局──────────→│ DELETE 角色+事件 或 重新 INSERT
```

### 数据安全

- **apiKey 加密**：后端使用 AES-GCM 将 apiKey 加密后存入 `life_llm_config`，读取时解密
- **localStorage 明文缓存**：前端将 apiKey 明文存入 `localStorage`（key 格式 `lifeSim:<deviceId>:apiKey`），用于"测试连接"和"下次回来恢复完整 Key"（后端只返回脱敏后的 Key）
- **deviceId**：前端生成 `device-` 前缀的唯一标识，写入 `localStorage`，用于关联用户设备与后端数据
- **删除操作**：前端提供"重新开始"（删角色+事件）和"删除所有数据"（删 LLM 配置+所有角色+所有事件+用户记录）两级删除

### API Key 的存储策略

前端和后端对 apiKey 采用不同的存储和处理方式：

- **后端**：apiKey 使用 AES-GCM 加密后存入 `life_llm_config.api_key` 字段，读取时解密后直接传给 AI 接口
- **前端 localStorage**：明文保存 apiKey，用于用户在配置面板查看/编辑/测试时不需要每次重新输入
- **脱敏返回**：`GET /api/life/llm/config` 返回的 apiKey 经过脱敏处理（只显示前 4 位 + `****`）
- **恢复逻辑**：配置面板 `onMounted` 时优先从 localStorage 恢复明文 apiKey，再从后端取脱敏 Key 兜底

### 存档机制

- **单存档**：每个设备（`deviceId`）只能有一个存活角色，开始新游戏会自动删除旧存档
- **断点恢复**：页面刷新后，从 `localStorage` 读取 `characterId`，调用 `/api/life/state` 恢复状态，再拉取最近事件还原剧情
- **存档失效处理**：如果后端角色已不存在或已死亡，前端自动返回配置面板

## 本地开发

### 环境要求

- Node.js 20+
- Java 17
- Maven 3.9+
- MySQL 8

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

### 后端启动

```bash
cd backend
mvn spring-boot:run
```

默认开发环境：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`

Vite 会把这些前缀代理到后端：

- `/clap`
- `/images`
- `/uploads`
- `/musics`
- `/posts`
- `/ai`
- `/users`
- `/visitor-stats`
- `/api/life`

## 构建

### 前端

```bash
cd frontend
npm install
npm run build
```

构建产物目录：

- `frontend/dist`

### 后端

```bash
cd backend
mvn clean package -DskipTests
```

Jar 默认输出到：

- `backend/target/personal-blog-0.0.1-SNAPSHOT.jar`

## 生产部署

当前推荐部署结构：

- Nginx 提供前端静态文件
- Spring Boot Jar 运行在 `127.0.0.1:8080`
- MySQL 只监听本机或内网

### 前端部署

不要部署 `frontend/` 源码目录，必须部署 `frontend/dist/` 的完整内容。

`dist/` 中至少应包含：

- `index.html`
- `assets/`
- `Core/`
- `Framework/`
- `Resources/`
- `image/`
- `favicon.ico`

如果线上只有 `Core/Framework/Resources`，但没有 `index.html` 或 `assets/`，页面会直接 403 或只显示背景。

### Nginx 配置重点

- 站点根目录应指向前端静态目录，例如 `/var/www/personal-blog`
- SPA 必须启用：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

- `/ai/chat` 是 SSE，代理时不要限制为 `POST`
- 上传音乐或图片时，记得设置：

```nginx
client_max_body_size 50M;
```

### 反向代理路径

至少需要代理这些路径到后端：

- `/posts`
- `/images`
- `/uploads`
- `/musics`
- `/clap`
- `/users`
- `/visitor-stats`
- `/ai`
- `/api/life`

### 常见部署坑

- 不要只上传 `public` 资源，要上传整个 `dist`
- 不要让两个 Nginx `server` 同时绑定同一个域名
- 资源 404 时不要错误回退成死循环重定向
- 更新前端后，移动端和微信内置浏览器通常需要强制清缓存

## 后端环境变量

生产环境至少需要：

```bash
PORT=8080
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=blog_db
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
UPLOAD_PATH=/opt/personal-blog/uploads
CORS_ALLOWED_ORIGINS=https://your-domain.com
MULTIPART_MAX_FILE_SIZE=50MB
MULTIPART_MAX_REQUEST_SIZE=50MB
MIMO_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
MIMO_API_KEY=your_mimo_api_key
DASHSCOPE_API_KEY=your_dashscope_api_key
```

## 上传目录

后端通过 [backend/src/main/java/com/azhi/config/WebConfig.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/config/WebConfig.java:1) 将：

- `/uploads/**`

映射到本地上传目录 `file.upload.path`。部署时需要提前创建该目录，并保证后端进程有读写权限。

## 统一响应格式

后端所有接口统一返回以下 JSON 结构（见 [Result.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/pojo/Result.java:1)）：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `Integer` | 200 表示成功，400 表示客户端错误，500 表示服务器错误 |
| `message` | `String` | 成功时固定 `"success"`，失败时包含错误描述 |
| `data` | `T`（泛型） | 实际返回的业务数据，可以是对象、数组或 null |

前端 [request.js](/abs/path/D:/personal-blog/frontend/src/api/request.js:1) 通过 Axios 响应拦截器自动解包：`code === 200` 时返回 `data`，其他情况以 `Promise.reject` 抛出，上层可统一 `catch`。

## BBS 论坛

BBS 是站点核心互动模块，支持发帖、编辑、删除和图片附件。前端页面位于 [BbsPage.vue](/abs/path/D:/personal-blog/frontend/src/pages/BbsPage.vue:1)，后端控制器位于 [PostController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/PostController.java:1)。

### 数据结构

帖子包含标题（`title`）、内容（`content`）、图片路径（`image_path`）和删除密钥（`delete_key`）。删除时需提供创建时生成的 `deleteKey`，防止被他人恶意删除。

### 权限模型

| 操作 | 条件 | 说明 |
|------|------|------|
| 查看帖子 | 无需登录 | 所有人可见 |
| 创建帖子 | 需登录 | Session 中 `currentUser` 不为空 |
| 编辑帖子 | 需提供 `deleteKey` | 编辑时校验删除密钥 |
| 删除帖子 | `deleteKey` 或管理员 | 普通用户凭密钥删除；管理员（`username = "AZHI4514"`）可删除任意帖子 |

### 编辑流程

- 调用 `PUT /posts/{postId}`，请求体包含 `deleteKey`、`title`、`content`、`imagePath`
- 后端校验 `deleteKey` 匹配后更新帖子记录

## 画廊

画廊页展示图片集合，前端页面位于 [GalleryPage.vue](/abs/path/D:/personal-blog/frontend/src/pages/GalleryPage.vue:1)，后端控制器位于 [GalleryController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/GalleryController.java:1)。

### 功能

| 操作 | 端点 | 说明 |
|------|------|------|
| 查看所有图片 | `GET /images` | 返回图片列表（含路径和元信息） |
| 添加图片 | `POST /images` | 管理员后台操作，接受 `{ imageUrl, description }` |
| 删除图片 | `DELETE /images/{imageId}` | 管理员后台操作 |

### 图片上传

图片文件通过 `POST /uploads/images` 上传，后端保存到 `file.upload.path` 目录并返回相对路径。上传接口见 [UploadController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/UploadController.java:1)。

## 音乐播放器

音乐页提供播放控制、封面展示和列表管理。前端页面位于 [MusicPage.vue](/abs/path/D:/personal-blog/frontend/src/pages/MusicPage.vue:1)，后端控制器位于 [MusicController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/MusicController.java:1)。

### 功能

| 操作 | 端点 | 说明 |
|------|------|------|
| 获取音乐列表 | `GET /musics` | 返回所有音乐（标题、演唱者、文件路径、封面路径） |
| 添加音乐 | `POST /musics` | 管理员添加音乐条目 |
| 删除音乐 | `DELETE /musics/{musicId}` | 管理员删除音乐 |

### 音乐上传

音乐文件通过 `POST /uploads/musics` 上传，支持 MP3 格式。文件保存到上传目录后，路径记录在 `music` 表中。

前端使用 HTML5 `<audio>` 实现播放控制，支持顺序播放和点击切换。页面展示封面图和当前播放曲目信息。

## 鼓掌/点赞

一个轻量互动模块，访客可为站点点击鼓掌。后端控制器位于 [ClapController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/ClapController.java:1)。

- 端点：`POST /clap`
- 无需登录，每次请求鼓掌计数 +1
- Clap 数据持久化在 `clap` 表，可用于展示站点点赞数

## 访客统计

记录并展示站点总访问量。后端控制器位于 [VisitorStatsController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/VisitorStatsController.java:1)。

| 操作 | 端点 | 说明 |
|------|------|------|
| 记录访问 | `POST /visitor-stats/record` | 记录一次访问（按 IP 去重），返回总访问量 |
| 查询总数 | `GET /visitor-stats/total` | 仅返回当前总访问量，不记录 |

IP 获取优先级：`X-Forwarded-For` > `X-Real-IP` > `RemoteAddr`，适配 Nginx 反向代理场景。

## 管理员后台

管理员页面位于 [AdminPage.vue](/abs/path/D:/personal-blog/frontend/src/pages/AdminPage.vue:1)，提供站点内容管理入口。当前管理员通过用户名硬编码识别（`username = "AZHI4514"`）。

管理功能包括：
- 图片上传与画廊管理（添加/删除图片）
- 音乐上传与音乐管理（添加/删除音乐）
- 帖子管理（管理员可删除任意帖子，无需 `deleteKey`）

管理员身份判断逻辑分布于各控制器中，核心判断位于 [PostController.deletePost()](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/PostController.java:39)。

## 链接与规则页

### 友情链接（LinksPage）

链接页位于 [LinksPage.vue](/abs/path/D:/personal-blog/frontend/src/pages/LinksPage.vue:1)，展示外部友情链接列表。当前为静态页面，链接数据直接维护在前端组件中。

路由：`/links`

### 站规（RulesPage）

规则页位于 [RulesPage.vue](/abs/path/D:/personal-blog/frontend/src/pages/RulesPage.vue:1)，展示站点规则说明。当前为静态页面，内容直接维护在前端组件中。

路由：`/rules`

## LangChain4j 与 MCP 集成

当前 AI 对话模块基于 LangChain4j 构建，核心架构包含以下几层。

### AI Service 接口

对话接口定义在 [AiCodeHelperService.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/AiCodeHelperService.java:1)，使用 LangChain4j 声明式 AI Service 模式：

- `@SystemMessage(fromResource = "system-prompt.txt")`：从 classpath 加载系统提示词，定义 AI 角色为"星辰观测站游戏角的房间伙伴 Yachiyo"
- `@MemoryId`：按 `memoryId` 隔离对话记忆，每个会话独立存储
- `@InputGuardrails`：绑定输入安全护栏
- 支持三种交互模式：
  - `chat()`：普通对话
  - `chatStream()`：`Flux<String>` 流式输出
  - `chatForReport()`：结构化输出（学习报告）

### 服务构建

AI Service 实例在 [AiCodeHelperServiceImpl.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/impl/AiCodeHelperServiceImpl.java:1) 中通过 `AiServices.builder()` 创建，关键组件：

| 组件 | 说明 |
|------|------|
| `ChatModel` | LangChain4j 自动注入的非流式聊天模型（`mimo-v2.5`） |
| `StreamingChatModel` | 流式聊天模型，用于 SSE 实时推送 |
| `MessageWindowChatMemory` | 滑动窗口记忆，每个会话保留最近 10 条消息 |
| `McpToolProvider` | MCP 工具提供者，为 AI 提供联网搜索能力 |

### MCP 工具集成

MCP 配置位于 [McpConfig.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/config/McpConfig.java:1)，通过阿里云 DashScope MCP 服务为 AI 提供 Web 搜索工具：

- 协议：Streamable HTTP（`StreamableHttpMcpTransport`）
- 目标：`dashscope.aliyuncs.com/api/v1/mcps/WebSearch/mcp`
- 认证：Bearer Token（`DASHSCOPE_API_KEY` 环境变量）
- 工具仅在 AI 判断需要搜索时才调用（见系统提示词第 5 条）

### 安全输入护栏

[SafeInputGuardrail.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/impl/SafeInputGuardrail.java:1) 实现 LangChain4j 的 `InputGuardrail` 接口，在用户消息进入 AI 模型前进行敏感词过滤。

- 所有检查不区分大小写
- 按单词边界（`\W+`）分割输入文本
- 命中敏感词时返回 `fatal()`，阻止消息送达模型
- 当前敏感词列表：`kill`, `die`, `suicide`, `death`, `murder`, `assault`, `attack`, `shoot` 及相关短语

### SSE 流式输出

AI 对话控制器 [AiControlller.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/AiControlller.java:1) 使用 Spring WebFlux 的 `Flux<ServerSentEvent<String>>` 实现 SSE：

- 请求：`GET /ai/chat?memoryId=N&message=...`
- 响应：`text/event-stream`
- 事件类型：`message`（内容块）、`done`（结束标记 `[DONE]`）、`error`（错误信息）
- 前端通过 `EventSource` 或 `fetch` + `ReadableStream` 消费流

### 系统提示词

系统提示词文件 [system-prompt.txt](/abs/path/D:/personal-blog/backend/src/main/resources/system-prompt.txt:1) 定义了 AI 的角色和行为规范：

1. 角色身份：站长为 AZHI4514，AI 名为 Yachiyo
2. 服务定位：星辰观测站游戏角的房间伙伴
3. 语气要求：温和、自然、简洁，有陪伴感
4. 安全边界：理解情绪、清晰解答、不夸张、不失控
5. 工具调用：仅在明确要求搜索时才调用联网功能

## 全局异常处理

后端使用 `@RestControllerAdvice` 全局拦截异常，统一返回 `Result` 格式的错误响应。

实现文件：[GlobalExceptionHandler.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/GlobalExceptionHandler.java:1)

当前覆盖的异常类型：

| 异常 | HTTP 状态码 | 说明 |
|------|------------|------|
| `IllegalArgumentException` | 400 | 参数校验失败或业务逻辑冲突（如未登录发帖、deleteKey 不匹配） |
| `IllegalStateException` | 500 | 服务器内部状态错误（如 AI 服务异常） |
| `MaxUploadSizeExceededException` | 400 | 上传文件超过 `MULTIPART_MAX_FILE_SIZE` 限制 |

前端 Axios 响应拦截器配合处理，当 `code !== 200` 时统一转换为 `Promise.reject`，上层业务代码集中 `catch` 即可。

## 后端 API 接口汇总

以下按模块列出所有后端 API 端点，标注认证要求和对应控制器。

### BBS 论坛

| 方法 | 端点 | 说明 | 认证 | 控制器 |
|------|------|------|------|--------|
| `GET` | `/posts` | 获取所有帖子 | 无 | `PostController` |
| `POST` | `/posts` | 创建帖子 | 需登录 | `PostController` |
| `PUT` | `/posts/{postId}` | 编辑帖子 | 需 deleteKey | `PostController` |
| `DELETE` | `/posts/{postId}` | 删除帖子 | deleteKey 或管理员 | `PostController` |

### 画廊

| 方法 | 端点 | 说明 | 认证 | 控制器 |
|------|------|------|------|--------|
| `GET` | `/images` | 获取所有图片 | 无 | `GalleryController` |
| `POST` | `/images` | 添加图片 | 管理员 | `GalleryController` |
| `DELETE` | `/images/{imageId}` | 删除图片 | 管理员 | `GalleryController` |

### 音乐

| 方法 | 端点 | 说明 | 认证 | 控制器 |
|------|------|------|------|--------|
| `GET` | `/musics` | 获取音乐列表 | 无 | `MusicController` |
| `POST` | `/musics` | 添加音乐 | 管理员 | `MusicController` |
| `DELETE` | `/musics/{musicId}` | 删除音乐 | 管理员 | `MusicController` |

### 上传

| 方法 | 端点 | 说明 | 认证 | 控制器 |
|------|------|------|------|--------|
| `POST` | `/uploads/images` | 上传图片文件 | 管理员 | `UploadController` |
| `POST` | `/uploads/musics` | 上传音乐文件 | 管理员 | `UploadController` |

### 鼓掌与访客统计

| 方法 | 端点 | 说明 | 认证 | 控制器 |
|------|------|------|------|--------|
| `POST` | `/clap` | 鼓掌 +1 | 无 | `ClapController` |
| `POST` | `/visitor-stats/record` | 记录访问（IP 去重） | 无 | `VisitorStatsController` |
| `GET` | `/visitor-stats/total` | 查询总访问量 | 无 | `VisitorStatsController` |

### 用户

| 方法 | 端点 | 说明 | 认证 | 控制器 |
|------|------|------|------|--------|
| `POST` | `/users/register` | 注册 | 无 | `UserController` |
| `POST` | `/users/login` | 登录 | 无 | `UserController` |
| `POST` | `/users/logout` | 退出 | 无 | `UserController` |

### AI 对话

| 方法 | 端点 | 说明 | 认证 | 控制器 |
|------|------|------|------|--------|
| `GET` | `/ai/chat` | SSE 流式对话 | 无 | `AiControlller` |

### 人生模拟器

| 方法 | 端点 | 说明 | 认证 | 控制器 |
|------|------|------|------|--------|
| `POST` | `/api/life/llm/config` | 保存/更新 LLM 配置 | 无（deviceId 绑定） | `LifeController` |
| `GET` | `/api/life/llm/config` | 获取 LLM 配置（apiKey 脱敏） | 无 | `LifeController` |
| `POST` | `/api/life/llm/test` | 测试 LLM 连接 | 无 | `LifeController` |
| `POST` | `/api/life/start` | 初始化新角色（非流式） | 无 | `LifeController` |
| `POST` | `/api/life/start/stream` | 初始化新角色（SSE 流式） | 无 | `LifeController` |
| `POST` | `/api/life/action` | 提交选择（非流式） | 无 | `LifeController` |
| `POST` | `/api/life/action/stream` | 提交选择（SSE 流式） | 无 | `LifeController` |
| `GET` | `/api/life/state` | 获取角色当前状态 | 无 | `LifeController` |
| `GET` | `/api/life/events` | 获取事件历史（分页） | 无 | `LifeController` |
| `DELETE` | `/api/life/character` | 删除指定角色及事件 | 无 | `LifeController` |
| `DELETE` | `/api/life/user/data` | 删除用户全部数据 | 无 | `LifeController` |

## CORS 与静态资源配置

### CORS 跨域

[WebConfig.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/config/WebConfig.java:1) 配置了 CORS 跨域规则：

- 允许的源：通过 `CORS_ALLOWED_ORIGINS` 环境变量配置（逗号分隔，支持通配符模式，如 `https://*.example.com`）
- 允许的方法：`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
- 允许的头部：`*`
- 携带凭证：`allowCredentials(true)`，配合前端的 `withCredentials: true`

开发环境默认允许 `http://localhost:5173`。

### 静态资源映射

`WebConfig` 同时将 `/uploads/**` URL 路径映射到 `file.upload.path` 本地目录，使上传的图片和音乐文件可直接通过 URL 访问。

前端 `public/` 目录下的静态资源（Live2D 运行时、favicon、音乐文件等）在构建后直接复制到 `dist/`，由 Nginx 或 Vite 开发服务器直接提供，不经过后端。
