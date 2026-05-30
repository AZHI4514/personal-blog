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