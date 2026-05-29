# Personal Blog

A full-stack personal blog built with Vue 3 + Spring Boot.

## Tech Stack

### Frontend
- Vue 3 + Vite
- Vue Router + Pinia
- Axios

### Backend
- Java 17 + Spring Boot 3.5
- MyBatis
- MySQL
- Maven

## Project Structure

`
personal-blog/
鈹溾攢鈹€ frontend/    # Vue 3 frontend
鈹溾攢鈹€ backend/     # Spring Boot backend
鈹斺攢鈹€ blog_db_backup.sql
`

## Quick Start

### Prerequisites
- Node.js >= 20.19.0
- Java 17
- Maven
- MySQL 8+

### Database Setup

1. Create the database:
`sql
CREATE DATABASE blog_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
`

2. Import the backup:
`ash
mysql -u your_username -p blog_db < blog_db_backup.sql
`

### Backend

`ash
cd backend
# Edit src/main/resources/application.yml with your DB credentials
mvn spring-boot:run
`

The backend runs on http://localhost:8080.

### Frontend

`ash
cd frontend
npm install
npm run dev
`

The frontend dev server proxies API requests to http://localhost:8080.

## Environment Variables

Copy .env.example to .env and fill in your values. See .env.example for details.