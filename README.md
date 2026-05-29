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

### Requirements

- Node.js >= 20.19
- Java 17
- Maven
- MySQL 8+

### Database

```sql
CREATE DATABASE blog_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Import the backup:

```bash
mysql -u your_username -p blog_db < blog_db_backup.sql
```

### Backend

Set environment variables based on `.env.example`, then run:

```bash
cd backend
mvn spring-boot:run
```

Default backend address: `http://localhost:8080`

### Frontend

Optional: copy `frontend/.env.example` to `frontend/.env.local` if you need a custom API address.

```bash
cd frontend
npm install
npm run dev
```

Default frontend address: `http://localhost:5173`

## Production Notes

- Do not commit real database credentials or server IPs.
- Use `VITE_API_BASE_URL=/` in production and let Nginx proxy API routes.
- Prefer `DB_HOST=127.0.0.1` when backend and MySQL are on the same server.
- Set `UPLOAD_PATH` to an absolute server path.
- Restrict `CORS_ALLOWED_ORIGINS` to your real domain.
