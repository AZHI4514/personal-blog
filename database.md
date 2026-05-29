# 数据库连接配置指南

## 环境信息

| 项目 | 值 |
|------|-----|
| Linux服务器IP | 120.26.238.12 |
| MySQL端口 | 3306 |
| 数据库名 | blog_db |
| 用户名 | root |
| 密码 | 1234 |

---

## 第一步：在Linux上配置MySQL允许远程连接

SSH登录到Linux服务器（120.26.238.12），执行以下命令：

### 1.1 修改MySQL配置文件

```bash
sudo nano /etc/my.cnf.d/mysql-server.cnf
```

找到 `bind-address = 127.0.0.1` 这一行，修改(或者添加)为：

```ini
bind-address = 0.0.0.0
```

保存后重启MySQL服务：

```bash
sudo systemctl restart mysql
# 或者
sudo systemctl restart mysqld
```

### 1.2 创建允许远程访问的MySQL用户

```bash
mysql -u root -p
```

在MySQL命令行中执行：

```sql
-- 方式一：创建允许远程访问的用户（推荐）
CREATE USER 'root'@'%' IDENTIFIED BY '1234';
GRANT ALL PRIVILEGES ON blog_db.* TO 'root'@'%';
FLUSH PRIVILEGES;

-- 方式二：直接修改现有root用户的host（简单但不推荐生产环境）
UPDATE mysql.user SET host='%' WHERE user='root' AND host='localhost';
FLUSH PRIVILEGES;
```

### 1.3 验证MySQL远程连接

在本地电脑测试：

```bash
mysql -h 120.26.238.12 -u root -p
```

---

## 第二步：修改Spring Boot后端配置

修改文件：`personal-blog-back/src/main/resources/application.yml`

将数据库连接URL从：

```yaml
url: jdbc:mysql://localhost:3306/blog_db
```

修改为：

```yaml
url: jdbc:mysql://192.168.88.130:3306/blog_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
```

完整配置示例：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
url: jdbc:mysql://120.26.238.12:3306/blog_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 1234
```

---

## 第三步：测试连接

1. 启动后端：在 `personal-blog-back` 目录运行 `mvn spring-boot:run`
2. 启动前端：在 `personal-blog-front` 目录运行 `npm run dev`
3. 访问前端页面，测试留言板、图片墙等功能是否正常

---

## 常见问题排查

| 问题 | 解决方案 |
|------|----------|
| 连接超时 | 检查Linux防火墙 `sudo ufw status` 或 `sudo firewall-cmd --list-all` |
| Access denied | 确认MySQL用户权限 `SELECT user, host FROM mysql.user;` |
| Public Key Retrieval | 在URL后添加 `&allowPublicKeyRetrieval=true` |
| 时区错误 | 确保URL中包含 `serverTimezone=Asia/Shanghai` |
