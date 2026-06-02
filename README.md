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

## Live2D SDK Deployment and Motion Setup

This project integrates Live2D directly inside `frontend/src/App.vue` without creating a separate Vue component.

### 1. Directory layout used by this project

Keep the original SDK and raw model files in the repo root:

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

Runtime files actually used by the frontend:

- `frontend/public/Core/live2dcubismcore.js`
- `frontend/public/Framework/Shaders/WebGL/*`
- `frontend/public/Resources/Yachiyo/*`

Source files used during build:

- `Live2d/CubismSdkForWeb-5-r.5/Framework/src/*`
- `Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src/*`

### 2. Copy the SDK runtime files into `frontend/public`

The browser cannot load files from the repo root directly at runtime, so the runtime assets must be copied into `frontend/public`.

Required copies:

```text
Live2d/CubismSdkForWeb-5-r.5/Core
  -> frontend/public/Core

Live2d/CubismSdkForWeb-5-r.5/Framework/Shaders
  -> frontend/public/Framework/Shaders

Live2d/Yachiyo
  -> frontend/public/Resources/Yachiyo
```

The project also uses these sample background resources:

```text
Live2d/CubismSdkForWeb-5-r.5/Samples/Resources/back_class_normal.png
  -> frontend/public/Resources/back_class_normal.png

Live2d/CubismSdkForWeb-5-r.5/Samples/Resources/icon_gear.png
  -> frontend/public/Resources/icon_gear.png
```

### 3. Configure Vite aliases for the official SDK source

`frontend/vite.config.js` needs two aliases:

- `@framework`
  points to `../Live2d/CubismSdkForWeb-5-r.5/Framework/src`
- `@live2d-demo`
  points to `../Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src`

This project imports the official sample classes directly from those paths instead of copying sample source code into `frontend/src`.

It also enables:

- `server.fs.allow: ['..']`

That is required because the alias target is outside `frontend/`.

### 4. How `App.vue` mounts the Live2D model

The current integration is all inside `frontend/src/App.vue`.

Main pieces:

- Add `watch` and `nextTick` imports from Vue.
- Add `live2dCanvas`, `live2dError`, and a few runtime variables for the SDK instance.
- Add `ensureLive2dCoreLoaded()` to inject `/Core/live2dcubismcore.js` into the page only once.
- Add `loadLive2dSdk()` to dynamically import:
  - `@framework/live2dcubismframework`
  - `@live2d-demo/lapppal`
  - `@live2d-demo/lappdefine`
  - `@live2d-demo/lappsubdelegate`
  - `@live2d-demo/lappview`
- Patch `LAppView.prototype.initializeSprite()` so the sample renderer does not depend on the original sample background setup.
- Patch `LAppSubdelegate.prototype.update()` so `gl.clearColor(0, 0, 0, 0)` makes the canvas background transparent.
- Force `live2dDefine.ModelDir` to use `['Yachiyo']`.
- Mount the model only when `currentPage === 'games'`.
- Destroy the renderer and animation frame when leaving the game page or unmounting the app.

The template side only needs a canvas mount point:

```html
<div v-if="currentPage === 'games'" class="game-container">
  <div class="live2d-stage">
    <canvas ref="live2dCanvas" class="live2d-canvas"></canvas>
    <div v-if="live2dError" class="live2d-error">{{ live2dError }}</div>
  </div>
</div>
```

Important page-name detail:

- the menu uses `showPage('games')`
- the page block must also use `currentPage === 'games'`

If one side is `game` and the other side is `games`, the model will never mount.

### 5. Why the model config file had to be fixed

The runtime model config is:

- `frontend/public/Resources/Yachiyo/Yachiyo.model3.json`

In this project, the model initially showed only a black screen because the file names inside `Yachiyo.model3.json` were mojibake and did not match the real files on disk.

The stable fix used here is to write the file names with Unicode escapes, for example:

```json
{
  "Moc": "\u516b\u5343\u4ee3\u8f89\u591c\u59ec.moc3"
}
```

That avoids path corruption while still resolving to the correct Chinese file names.

If the model loads as an empty or black canvas again, check this file first.

### 6. Current expression and motion behavior

The current runtime behavior comes from the official sample manager logic:

- dragging the model updates face/body follow behavior
- tapping the `Head` hit area triggers a random expression
- tapping the `Body` hit area tries to play a motion from the `TapBody` motion group

Current Yachiyo model status:

- expressions are configured
- hit areas are configured
- no motion files are currently listed in `Yachiyo.model3.json`

That means:

- head tap has visible effect
- body tap may have no visible effect until motions are added

### 7. How to configure expressions

Expressions are declared in `frontend/public/Resources/Yachiyo/Yachiyo.model3.json`:

```json
"Expressions": [
  { "Name": "tear", "File": "\u773c\u6cea.exp3.json" },
  { "Name": "smile_eye", "File": "\u772f\u772f\u773c.exp3.json" },
  { "Name": "smile", "File": "\u7b11\u54aa\u54aa.exp3.json" },
  { "Name": "blink", "File": "\u6cea\u73e0.exp3.json" }
]
```

To add a new expression:

1. Put the new `.exp3.json` file into `frontend/public/Resources/Yachiyo/`.
2. Add a new entry under `"Expressions"` in `Yachiyo.model3.json`.
3. Keep the file name exact. If the name contains Chinese characters, prefer Unicode escape form in JSON.

The sample code already calls `setRandomExpression()`, so any new expression listed here becomes available to random head-tap switching.

### 8. How to configure motions

If you want body tap to play an animation, the model config must declare motion groups.

Typical structure:

```json
"Motions": {
  "Idle": [
    { "File": "motions/idle_01.motion3.json" }
  ],
  "TapBody": [
    { "File": "motions/tap_01.motion3.json" }
  ]
}
```

Steps:

1. Put the `.motion3.json` files into `frontend/public/Resources/Yachiyo/` or a subfolder such as `motions/`.
2. Add the `"Motions"` section to `Yachiyo.model3.json`.
3. Make sure the group name matches the sample constant used by the runtime:
   - `Idle`
   - `TapBody`
4. Reload the frontend.

The current `App.vue` integration does not hardcode motion file names. It relies on the motion groups declared in `Yachiyo.model3.json`.

### 9. How to change click behavior

The click behavior is controlled by the official sample manager in:

- `Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src/lapplive2dmanager.ts`

Current sample logic:

- `Head` -> `setRandomExpression()`
- `Body` -> `startRandomMotion(MotionGroupTapBody, ...)`

If you want different behavior, there are two main ways:

1. Keep the sample logic and only change `Yachiyo.model3.json`
   - best when you just want to add expressions or motion files
2. Patch the imported sample class behavior in `App.vue`
   - best when you want custom click rules or a completely different trigger flow

Examples of custom changes:

- change body tap from `TapBody` to `Idle`
- always play one specific motion instead of random
- map head tap to a fixed expression such as `smile`
- disable tap-triggered motion entirely

### 10. Transparent background and page frame

The transparent canvas effect in this project is not a CSS-only change.

Two things are required:

1. CSS:
   - `.live2d-canvas { background: transparent; }`
2. WebGL clear alpha:
   - `gl.clearColor(0.0, 0.0, 0.0, 0.0)`

If only CSS is transparent but WebGL still clears with alpha `1`, the model area will still look black.

The surrounding frame is styled in `App.vue` to match the blog's BBS/post-form visual language:

- outer wrapper: `.game-container`
- inner panel: `.live2d-stage`

### 11. Troubleshooting checklist

If the model does not appear:

1. Confirm the page condition is `currentPage === 'games'`.
2. Confirm `frontend/public/Core/live2dcubismcore.js` exists.
3. Confirm `frontend/public/Framework/Shaders/WebGL/*` exists.
4. Confirm `frontend/public/Resources/Yachiyo/Yachiyo.model3.json` exists.
5. Confirm the file names inside `Yachiyo.model3.json` exactly match the real files.
6. Confirm the browser network panel can load:
   - `/Core/live2dcubismcore.js`
   - `/Resources/Yachiyo/Yachiyo.model3.json`
   - referenced `.moc3`, `.physics3.json`, `.exp3.json`, textures
7. If you see only a black rectangle, check the model JSON paths first.
8. If you see the model but body click does nothing, check whether the `"Motions"` section exists.

### 12. Recommended workflow when replacing the model

When switching from `Yachiyo` to another model:

1. Copy the new model folder into `frontend/public/Resources/<ModelName>/`.
2. Make sure the `<ModelName>.model3.json` file is present.
3. Fix any mojibake file names in the JSON.
4. Update the `ModelDir` override in `App.vue`:

```js
live2dDefine.ModelDir.splice(0, live2dDefine.ModelDir.length, '<ModelName>')
```

5. Rebuild:

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
