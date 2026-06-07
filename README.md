# 星尘观测站

一个以前端单页博客为主体、后端提供内容管理与 AI 对话能力的个人站点项目。当前包含首页、个人资料、画廊、音乐页、BBS、Live2D 游戏角，以及基于 SSE 的聊天助手。

## 技术栈

### 前端

- Vue 3
- Vite 7
- Vue Router
- Pinia
- Axios
- Live2D Cubism SDK for Web 5
- `@vitejs/plugin-legacy`（用于旧版移动浏览器兼容）

### 后端

- Java 17
- Spring Boot 3.5.x
- MyBatis
- MySQL 8
- Spring Security Crypto
- LangChain4j
- Reactor

## 目录结构

```text
personal-blog/
├─ frontend/                  # 前端工程
│  ├─ public/                 # 直接对外提供的静态资源
│  ├─ src/                    # Vue 源码
│  ├─ package.json
│  └─ vite.config.js
├─ backend/                   # Spring Boot 后端
│  ├─ src/main/java/com/azhi/
│  ├─ src/main/resources/
│  └─ pom.xml
├─ Live2d/                    # 原始 Live2D SDK 与模型素材
├─ deploy/                    # 部署示例配置
└─ README.md
```

说明：

- `frontend/` 和 `backend/` 是当前实际运行代码。
- `frontend/public/` 保存前端运行时实际访问的 Live2D 资源副本。
- `Live2d/` 保存原始 SDK 与模型素材，构建时通过 Vite alias 引用其中的 TypeScript 源码。

## 主要功能

- 复古风个人主页与导航
- 个人资料页 / 100 问 100 答
- 画廊展示与后台上传
- 音乐列表、播放控制、封面展示
- BBS 发帖、回复、编辑、删除
- 基于 Session 的用户登录 / 注册
- Live2D 游戏角
- `/ai/chat` SSE 流式对话

## 前端说明

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

### Live2D 运行时资源

前端运行时依赖这些资源：

- `frontend/public/Core/live2dcubismcore.js`
- `frontend/public/Framework/Shaders/WebGL/*`
- `frontend/public/Resources/Yachiyo/*`
- `frontend/public/Resources/back_class_normal.png`
- `frontend/public/Resources/icon_gear.png`

构建阶段依赖的 SDK 源码路径：

- `Live2d/CubismSdkForWeb-5-r.5/Framework/src`
- `Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src`

对应别名配置位于 [frontend/vite.config.js](/abs/path/D:/personal-blog/frontend/vite.config.js:1)。

### Live2D 自定义与问题排查

当前站点的 Live2D 逻辑主要集中在 [frontend/src/App.vue](/abs/path/D:/personal-blog/frontend/src/App.vue:1) 中，运行时模型入口为 [frontend/public/Resources/Yachiyo/Yachiyo.model3.json](/abs/path/D:/personal-blog/frontend/public/Resources/Yachiyo/Yachiyo.model3.json:1)。

这次移动端渲染问题的现象是：

- 模型动作和点击反馈正常
- 但部分手机浏览器只显示黑色轮廓，材质贴图没有正常显示

这类问题通常不是动作系统坏了，而是贴图资源或移动端 WebGL 兼容性问题。当前项目已经针对这类情况做了两项处理：

- 运行时资源路径改为优先使用 ASCII 名称，避免部分移动端对中文路径兼容不稳定
- Live2D 运行时贴图尺寸从原始超大纹理降到更适合移动端的级别，降低旧手机或 WebView 因最大纹理尺寸不足而导致贴图发黑的概率

如果后续再次出现 Live2D 黑轮廓、贴图缺失、但动作仍正常的情况，优先检查：

- `frontend/public/Resources/Yachiyo/Yachiyo.model3.json` 中引用的贴图路径是否真实存在
- `frontend/public/Resources/Yachiyo/textures/` 下的 `texture_00.png` 和 `texture_01.png` 是否已经部署到线上
- 线上是否重新执行了前端构建，并把整个 `frontend/dist/.` 完整复制到站点目录

如果要自定义模型动作、表情或资源，可按下面的方向修改：

- 修改模型入口：
  `frontend/public/Resources/Yachiyo/Yachiyo.model3.json`
- 替换表情文件：
  `frontend/public/Resources/Yachiyo/*.exp3.json`
- 替换物理参数：
  `frontend/public/Resources/Yachiyo/*.physics3.json`
- 替换贴图资源：
  `frontend/public/Resources/Yachiyo/textures/*.png`

当前点击角色触发表情、进入页面加载模型、以及拖动跟随指针等逻辑，主要在 [frontend/src/App.vue](/abs/path/D:/personal-blog/frontend/src/App.vue:868) 之后的 Live2D 初始化代码中。如果要继续自定义：

- 表情触发逻辑可调整 `LAppLive2DManager.prototype.onTap`
- 模型目录可调整 `live2dDefine.ModelDir`
- 加载中的提示文案与行为可调整 `mountLive2d` 和 `live2dLoading`

每次修改 Live2D 资源或前端逻辑后，都必须重新执行：

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

当前项目的登录认证是 **浏览器 Cookie + 后端 HttpSession** 方案，不是“前端拿到 JWT 后自己保存、自己解析”的那一套。

可以按下面这个顺序理解整套流程。

### 1. 用户注册或登录时，前端做了什么

前端通过 [frontend/src/api/user.js](/abs/path/D:/personal-blog/frontend/src/api/user.js:1) 调用这 3 个接口：

- `POST /users/register`
- `POST /users/login`
- `POST /users/logout`

所有请求最终都走 [frontend/src/api/request.js](/abs/path/D:/personal-blog/frontend/src/api/request.js:1) 里的 Axios 实例，这里有一个关键配置：

- `withCredentials: true`

它的作用是：

- 登录成功后，浏览器会保存后端返回的 Session Cookie
- 之后再请求同源接口时，浏览器会自动把这个 Cookie 带上
- 后端就能根据 Cookie 找回对应的 Session，从而识别“这是不是刚才那个已登录用户”

也就是说，**前端并不会把登录状态当成真正的认证依据保存在本地**，真正有效的登录态在后端 Session 里。

### 2. 后端登录成功后，怎么保存登录态

后端入口在 [backend/src/main/java/com/azhi/controller/UserController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/UserController.java:1)。

注册成功时：

- `POST /users/register`
- 调用 `userService.register(user)` 创建用户
- 然后执行 `session.setAttribute("currentUser", registeredUser)`

登录成功时：

- `POST /users/login`
- 调用 `userService.login(username, password)` 校验账号密码
- 然后执行 `session.setAttribute("currentUser", user)`

退出登录时：

- `POST /users/logout`
- 执行 `session.invalidate()`
- 当前 Session 失效，登录态随之清除

这里最关键的一行是：

- `session.setAttribute("currentUser", user)`

这表示后端把当前登录用户放进了 `HttpSession`，属性名叫 `currentUser`。后面凡是需要判断是否登录的接口，都是看这个值是否存在。

### 3. 密码是怎么校验的

密码逻辑在 [backend/src/main/java/com/azhi/service/impl/UserServiceImpl.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/impl/UserServiceImpl.java:1)。

- 注册时，后端使用 `BCryptPasswordEncoder` 对明文密码做哈希后再入库
- 登录时，后端先按用户名查用户，再用 `passwordEncoder.matches(...)` 比对密码
- 返回给前端之前，会把 `user.setPassword(null)`，避免把密码哈希返回出去

所以这个项目不是明文存密码，而是 **BCrypt 哈希存储**。

### 4. 后续接口怎么判断“用户已登录”

以发帖接口为例，代码在 [backend/src/main/java/com/azhi/controller/PostController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/PostController.java:1)。

创建帖子接口：

- `POST /posts`
- 方法参数里直接接收 `HttpSession session`
- 先执行 `requireLogin(session)`

`requireLogin(session)` 的判断逻辑很直接：

- 如果 `session.getAttribute("currentUser") == null`
- 就抛出“请先登录”

也就是说，后端并不会去检查某个前端变量是不是“已登录”，而是每次都从 Session 中取 `currentUser` 判断。

### 5. 为什么代码里还有 `Authorization` 头

[frontend/src/api/request.js](/abs/path/D:/personal-blog/frontend/src/api/request.js:1) 里还有这段兼容逻辑：

- 如果本地 `token` 存在，就带上 `Authorization: Bearer ...`

但从当前后端实现看：

- `UserController` 没有解析 JWT
- 登录接口也没有返回 token
- 权限判断实际依赖的是 `HttpSession` 里的 `currentUser`

所以 **当前项目真正生效的是 Session 认证**，`Authorization` 头更像是历史遗留或兼容代码，至少在现在这套后端实现里，它不是主认证链路。

### 6. 用一句话总结前后端分工

- 前端负责提交用户名密码，并在后续请求中通过 `withCredentials: true` 让浏览器自动携带 Session Cookie
- 后端负责校验密码、创建 Session、把登录用户写入 `currentUser`、并在受保护接口里读取 Session 判断是否已登录

## AI 对话

当前 AI 对话统一由前端直接请求后端 SSE 接口：

- `GET /ai/chat`
- 响应类型：`text/event-stream`

请求参数：

- `memoryId`
- `message`

后端核心入口与实现：

- [backend/src/main/java/com/azhi/controller/AiControlller.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/AiControlller.java:1)
- [backend/src/main/java/com/azhi/service/AiCodeHelperService.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/AiCodeHelperService.java:1)
- [backend/src/main/java/com/azhi/service/impl/AiCodeHelperServiceImpl.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/impl/AiCodeHelperServiceImpl.java:1)

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
