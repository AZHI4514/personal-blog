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

前端当前已启用 `@vitejs/plugin-legacy`，构建后会同时生成：

- 现代浏览器的 `type="module"` 入口
- 旧浏览器的 `nomodule` + `legacy` 回退脚本

这一步主要用于修复部分手机自带浏览器、老 WebView、部分微信内核只能看到背景但无法完成页面挂载的问题。

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

## 用户认证

当前站点使用 **Session 认证**，不是 JWT 主认证。

后端接口：

- `POST /users/register`
- `POST /users/login`
- `POST /users/logout`

前端请求实例配置：

- `withCredentials: true`
- 默认携带 Cookie

前端代码中仍保留了 `Authorization` 头兼容逻辑，但当前实际登录态仍以 `HttpSession` 为准。

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
