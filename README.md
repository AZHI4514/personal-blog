# Personal Blog

Vue 3 + Spring Boot personal blog, prepared for local development and production deployment.

## Stack

- Frontend: Vue 3, Vite, Vue Router, Pinia, Axios
- Backend: Spring Boot 3.5, MyBatis, MySQL

## Project Layout

```text
personal-blog/
├─ frontend/
└─ backend/
```

## Live2D SDK 部署与动作设置

这个项目将 Live2D 直接集成在 `frontend/src/App.vue` 中，没有额外创建单独的 Vue 组件。

### 1. 本项目使用的目录结构

将原始 SDK 和原始模型文件保留在仓库根目录：

```text
personal-blog/
├─ Live2d/
│  ├─ CubismSdkForWeb-5-r.5/
│  └─ Yachiyo/
├─ frontend/
│  ├─ public/
│  │  ├─ Core/
│  │  ├─ Framework/Shaders/
│  │  └─ Resources/Yachiyo/
│  └─ src/App.vue
```

前端运行时实际使用的文件：

- `frontend/public/Core/live2dcubismcore.js`
- `frontend/public/Framework/Shaders/WebGL/*`
- `frontend/public/Resources/Yachiyo/*`

构建时使用的源码文件：

- `Live2d/CubismSdkForWeb-5-r.5/Framework/src/*`
- `Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src/*`

### 2. 将 SDK 运行时文件复制到 `frontend/public`

浏览器在运行时不能直接从仓库根目录加载文件，所以必须把运行时资源复制到 `frontend/public`。

必须复制的内容：

```text
Live2d/CubismSdkForWeb-5-r.5/Core
  -> frontend/public/Core

Live2d/CubismSdkForWeb-5-r.5/Framework/Shaders
  -> frontend/public/Framework/Shaders

Live2d/Yachiyo
  -> frontend/public/Resources/Yachiyo
```

本项目还使用了这两个 sample 背景资源：

```text
Live2d/CubismSdkForWeb-5-r.5/Samples/Resources/back_class_normal.png
  -> frontend/public/Resources/back_class_normal.png

Live2d/CubismSdkForWeb-5-r.5/Samples/Resources/icon_gear.png
  -> frontend/public/Resources/icon_gear.png
```

### 3. 为官方 SDK 源码配置 Vite 别名

`frontend/vite.config.js` 需要两个别名：

- `@framework`
  指向 `../Live2d/CubismSdkForWeb-5-r.5/Framework/src`
- `@live2d-demo`
  指向 `../Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src`

这个项目直接从这些路径导入官方 sample 类，而不是把 sample 源码复制到 `frontend/src` 里。

同时还启用了：

- `server.fs.allow: ['..']`

这是必须的，因为别名目标目录位于 `frontend/` 外部。

### 4. `App.vue` 是如何挂载 Live2D 模型的

当前接入逻辑全部写在 `frontend/src/App.vue` 中。

主要部分如下：

- 从 Vue 中增加 `watch` 和 `nextTick` 导入。
- 增加 `live2dCanvas`、`live2dError`，以及若干 SDK 运行时变量。
- 增加 `ensureLive2dCoreLoaded()`，用于只向页面注入一次 `/Core/live2dcubismcore.js`。
- 增加 `loadLive2dSdk()`，用于动态导入：
  - `@framework/live2dcubismframework`
  - `@live2d-demo/lapppal`
  - `@live2d-demo/lappdefine`
  - `@live2d-demo/lappsubdelegate`
  - `@live2d-demo/lappview`
- Patch `LAppView.prototype.initializeSprite()`，让 sample 渲染器不再依赖原 sample 的背景初始化逻辑。
- Patch `LAppSubdelegate.prototype.update()`，让 `gl.clearColor(0, 0, 0, 0)` 把画布背景清成透明。
- 强制将 `live2dDefine.ModelDir` 设为 `['Yachiyo']`。
- 只在 `currentPage === 'games'` 时挂载模型。
- 在离开游戏页或卸载应用时销毁渲染器和动画帧。

模板侧只需要一个 canvas 挂载点：

```html
<div v-if="currentPage === 'games'" class="game-container">
  <div class="live2d-stage">
    <canvas ref="live2dCanvas" class="live2d-canvas"></canvas>
    <div v-if="live2dError" class="live2d-error">{{ live2dError }}</div>
  </div>
</div>
```

一个很重要的页面名细节：

- 菜单点击使用的是 `showPage('games')`
- 页面区块判断也必须使用 `currentPage === 'games'`

如果一边写成 `game`，另一边写成 `games`，模型就永远不会挂载。

### 5. 为什么必须修正模型配置文件

运行时使用的模型配置文件是：

- `frontend/public/Resources/Yachiyo/Yachiyo.model3.json`

在这个项目里，模型一开始只显示黑屏，是因为 `Yachiyo.model3.json` 里的文件名是乱码，和磁盘上的真实文件名不一致。

这里采用的稳定修复方式，是把文件名写成 Unicode 转义形式，例如：

```json
{
  "Moc": "\u516b\u5343\u4ee3\u8f89\u591c\u59ec.moc3"
}
```

这样既能避免路径因为编码问题损坏，又能正确解析到真实的中文文件名。

如果以后模型再次变成空白画布或黑画布，优先先检查这个文件。

### 6. 当前表情行为

当前项目中的运行时行为如下：

- 鼠标在 Live2D 画布上移动时，会更新角色的面部和身体跟随行为
- 鼠标离开画布时，会把跟随目标重置回中心
- 点击画布时，会触发一次随机面部表情

当前 Yachiyo 模型的状态：

- 已配置表情
- 本项目中已刻意禁用身体动作播放

这意味着角色现在只保留“指针跟随”和“表情切换”两类行为。

### 7. 如何配置表情

表情定义写在 `frontend/public/Resources/Yachiyo/Yachiyo.model3.json` 中：

```json
"Expressions": [
  { "Name": "tear", "File": "\u773c\u6cea.exp3.json" },
  { "Name": "smile_eye", "File": "\u772f\u772f\u773c.exp3.json" },
  { "Name": "smile", "File": "\u7b11\u54aa\u54aa.exp3.json" },
  { "Name": "blink", "File": "\u6cea\u73e0.exp3.json" }
]
```

如果要新增一个表情：

1. 把新的 `.exp3.json` 文件放进 `frontend/public/Resources/Yachiyo/`。
2. 在 `Yachiyo.model3.json` 的 `"Expressions"` 下增加一条新记录。
3. 文件名必须完全一致；如果文件名包含中文，推荐在 JSON 里使用 Unicode 转义形式。

由于 sample 代码已经调用了 `setRandomExpression()`，所以这里新增的表情会自动加入到随机点击切换中。

### 8. 鼠标跟随是如何实现的

鼠标跟随逻辑写在 `frontend/src/App.vue` 中。

本项目的实现步骤如下：

1. 保留官方 `LAppSubdelegate` 渲染器。
2. 把 `pointermove` 绑定到 Live2D canvas，而不是只在拖拽时才追踪。
3. 将 canvas 内部局部坐标换算成 Live2D 视图坐标。
4. 在每次移动事件中调用 `subdelegate.getLive2DManager().onDrag(viewX, viewY)`。
5. 在 `pointerleave` 时调用 `onDrag(0.0, 0.0)`，让模型回到中性位置。

一个重要细节：

- 原始 sample 只会在指针被捕获时更新拖拽状态
- 这个项目重写了这部分行为，所以跟随是在悬停时生效，而不只是拖拽时生效

### 9. 点击触发随机表情是如何实现的

点击行为是基于官方 sample manager 进行 patch 的，原始位置在：

- `Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src/lapplive2dmanager.ts`

这个项目重写了 `LAppLive2DManager.prototype.onTap()`，让它只做一件事：

- 调用 `model.setRandomExpression()`

这也同时移除了 sample 里原本“点击身体触发动作”的行为。

实现步骤如下：

1. 在 `App.vue` 中导入 `@live2d-demo/lapplive2dmanager`。
2. Patch `LAppLive2DManager.prototype.onTap`。
3. 在 patch 后的函数中取出 `this._models[0]`。
4. 如果模型存在，则调用 `setRandomExpression()`。
5. 不再调用 `startRandomMotion(...)`。

### 10. 如何新增或修改点击表情

当前可用于随机点击切换的表情仍然来自：

- `frontend/public/Resources/Yachiyo/Yachiyo.model3.json`

如果你在 `"Expressions"` 下继续新增表情条目，随机点击行为会自动把它们包含进去。

如果你不想随机，而是想固定切换到某个表情，可以把：

```js
model.setRandomExpression()
```

替换成：

```js
model.setExpression('smile')
```

### 11. 透明背景与页面外框

这个项目里的透明画布效果，不是只改 CSS 就够了。

必须同时满足两件事：

1. CSS:
   - `.live2d-canvas { background: transparent; }`
2. WebGL clear alpha:
   - `gl.clearColor(0.0, 0.0, 0.0, 0.0)`

如果只有 CSS 是透明的，但 WebGL 仍然用 alpha `1` 清屏，那么模型区域看起来还是黑的。

外围框体样式同样写在 `App.vue` 中，用来和博客的 BBS / post-form 视觉风格保持一致：

- 外层包裹：`.game-container`
- 内层面板：`.live2d-stage`

### 12. 排错清单

如果模型没有显示：

1. 确认页面判断条件是 `currentPage === 'games'`。
2. 确认 `frontend/public/Core/live2dcubismcore.js` 存在。
3. 确认 `frontend/public/Framework/Shaders/WebGL/*` 存在。
4. 确认 `frontend/public/Resources/Yachiyo/Yachiyo.model3.json` 存在。
5. 确认 `Yachiyo.model3.json` 里的文件名与真实文件名完全一致。
6. 确认浏览器网络面板能够成功加载：
   - `/Core/live2dcubismcore.js`
   - `/Resources/Yachiyo/Yachiyo.model3.json`
   - 以及它引用的 `.moc3`、`.physics3.json`、`.exp3.json`、贴图文件
7. 如果你只看到黑色矩形，优先检查模型 JSON 里的路径。
8. 如果鼠标跟随失效，检查 canvas 的 `pointermove` 处理器是否还在调用 `onDrag(viewX, viewY)`。
9. 如果点击后脸部表情没有变化，检查 `"Expressions"` 区段是否存在，以及 `onTap()` 是否仍然被 patch 为 `setRandomExpression()`。

### 13. 替换模型时推荐的流程

当你要把 `Yachiyo` 替换成其他模型时：

1. 把新模型文件夹复制到 `frontend/public/Resources/<ModelName>/`。
2. 确保 `<ModelName>.model3.json` 文件存在。
3. 修正 JSON 中所有乱码文件名。
4. 更新 `App.vue` 中对 `ModelDir` 的覆盖：

```js
live2dDefine.ModelDir.splice(0, live2dDefine.ModelDir.length, '<ModelName>')
```

5. 重新构建：

```bash
cd frontend
npm run build
```

## Local Development

### 1. 进入服务器并安装环境

ssh root@你的服务器公网IP
apt update
apt upgrade -y
apt install -y git curl unzip nginx mysql-server openjdk-17-jdk maven
systemctl enable nginx
systemctl start nginx
systemctl enable mysql
systemctl start mysql
java -version
mvn -version
node -v
如果还没装 Node 20，再执行：

curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs
node -v
npm -v
### 2. clone 项目

cd /home
git clone https://github.com/AZHI4514/personal-blog.git
cd /home/personal-blog
### 3. 初始化 MySQL

mysql -uroot
进 MySQL 后执行：

CREATE DATABASE IF NOT EXISTS blog_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'blog_user'@'localhost' IDENTIFIED BY '你的数据库密码';
GRANT ALL PRIVILEGES ON blog_db.* TO 'blog_user'@'localhost';
FLUSH PRIVILEGES;
exit;
导入数据库：

mysql -u blog_user -p blog_db < /home/personal-blog/blog_db_backup.sql
### 4. 创建后端运行目录和上传目录

mkdir -p /opt/personal-blog/backend/uploads
mkdir -p /var/www/personal-blog
### 5. 构建前端

cd /home/personal-blog/frontend
npm install
npm run build
rm -rf /var/www/personal-blog/*
cp -r dist/* /var/www/personal-blog/
### 6. 构建后端

cd /home/personal-blog/backend
mvn clean package -DskipTests
cp target/personal-blog-0.0.1-SNAPSHOT.jar /opt/personal-blog/backend/app.jar
### 7. 创建 systemd 后端服务

cat >/etc/systemd/system/personal-blog-back.service <<'EOF'
[Unit]
Description=Personal Blog Backend
After=network.target mysql.service

[Service]
User=root
WorkingDirectory=/opt/personal-blog/backend
Environment="PORT=8080"
Environment="DB_HOST=127.0.0.1"
Environment="DB_PORT=3306"
Environment="DB_NAME=blog_db"
Environment="DB_USERNAME=blog_user"
Environment="DB_PASSWORD=你的数据库密码"
Environment="UPLOAD_PATH=/opt/personal-blog/backend/uploads"
Environment="CORS_ALLOWED_ORIGINS=https://你的域名"
ExecStart=/usr/bin/java -jar /opt/personal-blog/backend/app.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
### 8. 启动后端

systemctl daemon-reload
systemctl enable personal-blog-back
systemctl start personal-blog-back
systemctl status personal-blog-back
看日志：

journalctl -u personal-blog-back -f

## 游戏角 Agent 配置说明（新增）

本次接入只改了两个位置：

1. 前端只在 `frontend/src/App.vue` 的“游戏角”页面内嵌了 agent 界面，没有新建组件，也没有改动其他页面的结构。
2. 后端新增了独立的 `room-agent` 接口，用来给游戏角提供房间状态、长期记忆和 MCP 白名单调用能力，不影响原有的图片、音乐、BBS、用户等接口。

### 前端是怎么接入的

游戏角页面现在包含这几部分：

- Live2D 展示区：继续复用原来游戏角里的 Live2D 逻辑。
- 对话区：可以直接输入文本，也可以附带图片。
- 房间状态卡：会从后端读取当前房间状态并显示在 Live2D 区域上。
- 音乐信息卡：直接复用现有的音乐列表数据，不新增音乐表。
- 设置面板：也直接写在 `App.vue` 里，没有拆组件。

设置面板里目前可配置：

- `API URL`
- `API Key`
- `模型名`
- `视觉模式`
- 是否启用长期记忆
- 是否启用 MCP
- 角色知识库条目

这些前端设置目前都保存在浏览器本地 `localStorage` 中，使用的 key 是：

- `roomLLMSettings`
- `roomMemorySettings`
- `roomMCPSettings`
- `roomKnowledgeSettings`
- `roomChatHistory`

其中 `API Key` 保留为空，后续在游戏角页面的设置面板里填写即可。

### 后端是怎么接入的

本次新增了两个后端文件：

- `backend/src/main/java/com/azhi/controller/RoomAgentController.java`
- `backend/src/main/java/com/azhi/service/RoomAgentService.java`

并且只额外放开了 `PATCH` 跨域方法，方便记忆接口更新使用。

当前提供的接口如下：

- `GET /room-agent/world`
- `GET /room-agent/memory`
- `POST /room-agent/memory`
- `PATCH /room-agent/memory/{id}`
- `DELETE /room-agent/memory/{id}`
- `POST /room-agent/mcp/call`

### 长期记忆现在的实现方式

按照你的要求，这次**没有新增数据库表**。

当前长期记忆是后端内存版实现，特点是：

- 不需要你改数据库
- 服务重启后记忆会丢失
- 已经保留了后续替换成数据库版本的接口边界

也就是说，你现在可以先把功能跑通；如果以后你想把长期记忆持久化到 MySQL，再单独补表即可。

如果后续你决定改成数据库版，我再给你正式的建表 SQL，你建完我再帮你切过去。

### MCP 现在的实现方式

当前 MCP 接口是白名单占位实现，只允许这两个工具名：

- `understand_image`
- `web_search`

这样做是为了不破坏你原来的后端结构，也避免把敏感调用直接开放到前端。

### LLM 接入方式

当前默认支持两种模式：

1. 前端直接请求你填写的模型接口地址。
2. 勾选“使用服务端代理”后，走 `/api/chat` 代理模式。

注意：

- 这次我没有帮你新增 `/api/chat` 后端代理接口。
- 如果你后面想走代理模式，需要你现有后端再补一个代理接口，或者继续使用直连模式。

所以你现在最简单的用法是：

1. 打开“游戏角”
2. 打开设置面板
3. 填入模型的 `API URL`
4. 填入你自己的 `API Key`
5. 保存后直接聊天

### 这次接入没有改动的内容

为了避免破坏你原有结构，这次没有动这些部分：

- 没有新建前端组件
- 没有改你的路由结构
- 没有改图片页、音乐页、BBS 页、规则页等其他页面
- 没有改你现有数据库表
- 没有替换你原来的音乐、Live2D、用户登录等已有逻辑

### 你后续需要自己补的内容

要让这个 agent 真正接入大模型，你只需要补这一个核心信息：

- 在游戏角设置面板里填写真实的 `API URL` 和 `API Key`

如果你以后想继续增强，可以再做这些扩展：

- 把长期记忆从内存版改成 MySQL 持久化
- 新增后端 `/api/chat` 代理接口
- 把 `MCP` 的占位返回替换成真实工具调用
### 9. 配置 Nginx

cat >/etc/nginx/sites-available/personal-blog <<'EOF'
server {
    listen 80;
    server_name 你的域名;

    root /var/www/personal-blog;
    index index.html;
    client_max_body_size 10m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /posts {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /images {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /uploads {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /musics {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /clap {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /users {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /visitor-stats {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF
启用站点：

ln -sf /etc/nginx/sites-available/personal-blog /etc/nginx/sites-enabled/personal-blog
nginx -t
systemctl reload nginx
如果默认站点冲突：

rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
### 10. 开放安全组
放行这些端口：

22
80
443
不要对公网开放：

3306
8080
### 11. 本机检查
检查 MySQL：

mysql -u blog_user -p -h 127.0.0.1 blog_db
检查后端：

curl http://127.0.0.1:8080/posts
curl http://127.0.0.1:8080/images
curl http://127.0.0.1:8080/visitor-stats/total
检查前端：

curl http://127.0.0.1
### 12. 访问
浏览器打开：

http://你的域名
或

http://你的服务器公网IP



### 无域名、直接用公网 IP”的 Nginx 配置


先创建 Nginx 配置：

cat >/etc/nginx/sites-available/personal-blog <<'EOF'
server {
    listen 80;
    server_name _;

    root /var/www/personal-blog;
    index index.html;
    client_max_body_size 10m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /posts {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /images {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /uploads {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /musics {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /clap {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /users {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /visitor-stats {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF
然后启用配置：

ln -sf /etc/nginx/sites-available/personal-blog /etc/nginx/sites-enabled/personal-blog
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
如果你前端已经构建并复制到：

/var/www/personal-blog
那现在直接访问：

http://你的服务器公网IP
就可以了。

顺手检查这几个点：

systemctl status nginx
systemctl status personal-blog-back
curl http://127.0.0.1:8080/posts
curl http://127.0.0.1
如果页面能打开但接口没数据，再看后端日志：

journalctl -u personal-blog-back -f
