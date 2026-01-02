# User Management Service

> **Part of [Core Microservices Project](https://github.com/CoreWebMicroservices/corems-project)** - Enterprise-grade microservices toolkit for rapid application development

User authentication, authorization, and profile management microservice for CoreMS.

## Features

- User registration and authentication
- JWT token management
- Role-based access control (RBAC)
- OAuth2 integration (Google)
- User profile management

## Quick Start
```bash
# Clone the main project
git clone https://github.com/CoreWebMicroservices/corems-project.git
cd corems-project

# Build and start user service
./setup.sh build user-ms
./setup.sh start user-ms

# Or start entire stack
./setup.sh start-all
```

### API Endpoints
- **Base URL**: `http://localhost:3000`
- **Health**: `GET /actuator/health`
- **Auth**: `POST /api/auth/signin`
- **Profile**: `GET /api/profile/me`

## Environment Variables

Copy `.env-example` to `.env` and configure:
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/corems
DATABASE_USER=admin
DATABASE_PASSWORD=admin
AUTH_TOKEN_SECRET=your_jwt_secret
```

## Database Schema

- `user_ms` schema with tables:
  - `app_user` - User accounts
  - `app_user_role` - User roles
  - `login_token` - JWT tokens

## Architecture

```
user-ms/
├── user-api/          # OpenAPI spec + generated models
├── user-client/       # API client for other services
├── user-service/      # Main application
└── migrations/        # Database migrations
```