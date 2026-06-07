# Deployment Guide

## Keep Out Of Git

- Real database host, username, and password
- Production server IP notes
- Future secret keys or third-party credentials
- Runtime logs and uploaded files

## Backend Environment Variables

Example:

```bash
export PORT=8080
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=blog_db
export DB_USERNAME=blog_user
export DB_PASSWORD=replace_me
export UPLOAD_PATH=/opt/personal-blog/backend/uploads
export CORS_ALLOWED_ORIGINS=https://your-domain.com
```

If you use `systemd`, move these into the service config or an `EnvironmentFile`.

## Frontend Build

```bash
cd frontend
npm install
npm run build
```

Production default:

```bash
VITE_API_BASE_URL=/
```

## Backend Build

```bash
cd backend
mvn clean package -DskipTests
```

## Recommended Runtime Layout

- Frontend static files served by Nginx
- Backend jar managed by systemd
- MySQL listening on localhost or private network only
- Public internet does not expose port `3306`

## Reverse Proxy

Use `deploy/nginx.conf.example` as the starting point.
