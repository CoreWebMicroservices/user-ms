# User Management Service

> **Part of [Core Microservices Project](https://github.com/CoreWebMicroservices/corems-project)** - Enterprise-grade microservices toolkit for rapid application development

OAuth2/OIDC Authorization Server with user authentication, authorization, and profile management.

## Features

### OAuth2/OIDC Compliance
- **OAuth2 Authorization Server** with standard endpoints
- **OpenID Connect (OIDC)** provider with ID tokens
- **PKCE** (Proof Key for Code Exchange) for secure authorization code flow
- **Discovery endpoint** (`/.well-known/openid-configuration`)
- **JWKS endpoint** (`/.well-known/jwks.json`) for public key distribution

### Grant Types
- **Password Grant** - First-party applications (username/password)
- **Authorization Code Grant** - Third-party applications with PKCE
- **Refresh Token Grant** - Token renewal without re-authentication

### Authentication & Security
- User registration with email/phone verification
- Password reset flow
- JWT token management (HS256/RS256 algorithms)
- Role-based access control (RBAC)
- Token revocation (RFC 7009)
- Social OAuth2 (Google, GitHub, LinkedIn)

### User Management
- User profile management
- Admin user CRUD operations
- Searchable user directory with filtering

## Quick Start

```bash
# Clone the main project
git clone https://github.com/CoreWebMicroservices/corems-project.git
cd corems-project

# Start infrastructure (PostgreSQL)
./setup.sh infra

# Build and start user service
./setup.sh build user-ms
./setup.sh start user-ms

# Or start entire stack
./setup.sh start-all
```

## API Endpoints

**Base URL**: `http://localhost:3000`

### OAuth2/OIDC Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/.well-known/openid-configuration` | GET | OIDC discovery document |
| `/.well-known/jwks.json` | GET | JSON Web Key Set (public keys) |
| `/oauth2/authorize` | GET | Authorization endpoint (code flow) |
| `/oauth2/token` | POST | Token endpoint (all grant types) |
| `/oauth2/revoke` | POST | Token revocation endpoint |
| `/oauth2/userinfo` | GET | OIDC UserInfo endpoint |

### Registration & Verification

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/auth/signup` | POST | No | Register new user |
| `/api/auth/verify-email` | POST | No | Verify email with token |
| `/api/auth/verify-phone` | POST | No | Verify phone with code |
| `/api/auth/resend-verification` | POST | No | Resend verification code |

### Password Management

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/auth/forgot-password` | POST | No | Request password reset |
| `/api/auth/reset-password` | POST | No | Reset password with token |
| `/api/profile/change-password` | POST | Yes | Change own password |

### Profile Management

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/profile` | PATCH | Yes | Update own profile |

### Admin User Management

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/users` | GET | Yes | List all users (paginated) |
| `/api/users` | POST | Yes | Create new user |
| `/api/users/{userId}` | GET | Yes | Get user by ID |
| `/api/users/{userId}` | PUT | Yes | Update user |
| `/api/users/{userId}` | DELETE | Yes | Delete user |
| `/api/users/{userId}/change-password` | POST | Yes | Admin set user password |
| `/api/users/{userId}/change-email` | POST | Yes | Admin change user email |

## Authentication Examples

### Password Grant (First-Party Apps)

```bash
# Sign in with username/password
curl -X POST http://localhost:3000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "username=user@example.com" \
  -d "password=SecurePass123!" \
  -d "scope=openid profile email"

# Response
{
  "access_token": "eyJ0eXAiOiJhY2Nlc3NfdG9rZW4i...",
  "refresh_token": "eyJ0eXAiOiJyZWZyZXNoX3Rva2VuI...",
  "id_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 600,
  "scope": "openid profile email"
}
```

### Authorization Code Flow with PKCE (Third-Party Apps)

```bash
# 1. Generate PKCE code verifier and challenge
CODE_VERIFIER=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-43)
CODE_CHALLENGE=$(echo -n $CODE_VERIFIER | openssl dgst -sha256 -binary | base64 | tr -d "=+/" | cut -c1-43)

# 2. Redirect user to authorization endpoint
http://localhost:3000/oauth2/authorize?response_type=code&client_id=my-app&redirect_uri=http://localhost:8080/callback&scope=openid%20profile%20email&state=random-state&code_challenge=$CODE_CHALLENGE&code_challenge_method=S256

# 3. User authenticates and approves
# 4. Redirect back with authorization code: http://localhost:8080/callback?code=abc123&state=random-state

# 5. Exchange code for tokens
curl -X POST http://localhost:3000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=abc123" \
  -d "redirect_uri=http://localhost:8080/callback" \
  -d "code_verifier=$CODE_VERIFIER" \
  -d "client_id=my-app"
```

### Refresh Token

```bash
curl -X POST http://localhost:3000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=eyJ0eXAiOiJyZWZyZXNoX3Rva2VuI..."
```

### Get User Info

```bash
curl http://localhost:3000/oauth2/userinfo \
  -H "Authorization: Bearer eyJ0eXAiOiJhY2Nlc3NfdG9rZW4i..."

# Response
{
  "sub": "2a3e0b5c-2192-4612-9e02-693989dbb7e5",
  "email": "user@example.com",
  "email_verified": true,
  "given_name": "John",
  "family_name": "Doe",
  "picture": "https://example.com/avatar.jpg",
  "roles": ["USER_MS_USER"]
}
```

### Revoke Token (Sign Out)

```bash
curl -X POST http://localhost:3000/oauth2/revoke \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=eyJ0eXAiOiJyZWZyZXNoX3Rva2VuI..." \
  -d "token_type_hint=refresh_token"
```

## Environment Variables

Copy `.env-example` to `.env` and configure:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/corems
DATABASE_USER=admin
DATABASE_PASSWORD=admin

# JWT Configuration
AUTH_TOKEN_ALG=HS256                    # Algorithm: HS256, HS384, HS512, RS256, RS384, RS512
AUTH_TOKEN_ISSUER=http://localhost:3000 # Token issuer (must match your service URL)
JWT_KEY_ID=corems-1                     # Key ID for JWKS

# For HS256 (symmetric)
AUTH_TOKEN_SECRET=<base64-encoded-secret>  # Generate: openssl rand -base64 32

# For RS256 (asymmetric) - Recommended for production
# JWT_PRIVATE_KEY=<PEM-encoded-private-key>  # Generate: openssl genrsa -out private.pem 2048
# JWT_PUBLIC_KEY=<PEM-encoded-public-key>    # Extract: openssl rsa -in private.pem -pubout

# Frontend
FRONTEND_BASE_URL=http://localhost:8080

# OAuth2 Social Login (Optional)
GOOGLE_CLIENT_ID=<your-google-client-id>
GOOGLE_CLIENT_SECRET=<your-google-client-secret>
GITHUB_CLIENT_ID=<your-github-client-id>
GITHUB_CLIENT_SECRET=<your-github-client-secret>
```

### Generating Keys

**For HS256 (Development):**
```bash
# Generate 256-bit secret
openssl rand -base64 32
```

**For RS256 (Production):**
```bash
# Generate RSA private key
openssl genrsa -out private.pem 2048

# Extract public key
openssl rsa -in private.pem -pubout -out public.pem

# View keys (copy full content including headers)
cat private.pem
cat public.pem
```

## Database Schema

**Schema**: `user_ms`

| Table | Description |
|-------|-------------|
| `app_user` | User accounts with credentials |
| `app_user_role` | User role assignments |
| `login_token` | Refresh tokens (revocable) |
| `authorization_codes` | OAuth2 authorization codes (PKCE) |
| `action_token` | Email/phone verification tokens |

## Token Configuration

| Token Type | Default TTL | Algorithm | Revocable |
|------------|-------------|-----------|-----------|
| Access Token | 10 minutes | HS256/RS256 | No (stateless JWT) |
| Refresh Token | 24 hours | HS256/RS256 | Yes (database) |
| ID Token | 60 minutes | HS256/RS256 | No (stateless JWT) |
| Authorization Code | 10 minutes | N/A | Yes (one-time use) |

## ID Token Claims

ID tokens include claims based on requested scopes:

| Scope | Claims Included |
|-------|-----------------|
| `openid` | sub, iss, aud, exp, iat, auth_time, nonce |
| `profile` | given_name, family_name, picture, updated_at |
| `email` | email, email_verified |
| `phone` | phone_number, phone_number_verified |

## Roles

From `CoreMsRoles` enum:
- `USER_MS_ADMIN` - Full user management access
- `USER_MS_USER` - Standard user access
- `SUPER_ADMIN` - System-wide admin (access to all services)

## Architecture

```
user-ms/
├── user-api/          # OpenAPI spec + generated models
├── user-client/       # API client for other services
├── user-service/      # Main application
│   ├── controller/    # REST endpoints
│   ├── service/       # Business logic
│   ├── repository/    # Data access
│   ├── entity/        # JPA entities
│   └── config/        # Security & JWT config
└── migrations/        # Database migrations
    ├── setup/         # Schema (V1.0.x)
    └── mockdata/      # Seed data (R__xx)
```

## Development

### Build
```bash
# Build API models
./setup.sh build user-ms

# Run tests
cd repos/user-ms/user-service
mvn test

# Run integration tests
mvn verify -P integration-tests
```

### Database Migrations
```bash
# Run migrations
./setup.sh migrate

# Run with seed data
./setup.sh migrate --mockdata

# Clean and migrate (dev only)
./setup.sh migrate --mockdata --clean
```

### Logs
```bash
# View logs
./setup.sh logs user-ms

# Follow logs
./setup.sh logs user-ms -f
```

## Security Considerations

### HS256 vs RS256

**HS256 (Symmetric):**
- ✅ Simpler configuration (single secret)
- ✅ Faster signing/verification
- ❌ Secret must be shared with all services
- ❌ Cannot expose public key in JWKS
- ❌ Third-party clients cannot verify tokens
- **Use for:** Development, first-party apps only

**RS256 (Asymmetric):**
- ✅ Public key can be shared via JWKS
- ✅ Third-party clients can verify tokens
- ✅ Private key never leaves authorization server
- ✅ Supports key rotation
- ❌ Slightly slower than HS256
- **Use for:** Production, third-party OIDC clients

### Best Practices

1. **Use RS256 for production** with third-party clients
2. **Keep access tokens short-lived** (10 minutes)
3. **Implement refresh token rotation** (delete old on use)
4. **Use PKCE** for all authorization code flows
5. **Enable rate limiting** on token endpoint
6. **Log all authentication events** for audit trail
7. **Rotate keys regularly** (RS256 only)

## Troubleshooting

### Token Verification Fails
- Check `AUTH_TOKEN_ISSUER` matches your service URL
- Verify secret/keys are correctly configured
- Ensure algorithm matches (HS256 vs RS256)

### JWKS Returns Empty
- Normal for HS256 (symmetric keys not exposed)
- For RS256, verify `JWT_PUBLIC_KEY` is set

### Authorization Code Expired
- Codes expire after 10 minutes
- User must re-authenticate

### Refresh Token Invalid
- Token may have been revoked
- Check `login_token` table for token existence

## Links

- **Main Project**: https://github.com/CoreWebMicroservices/corems-project
- **OpenAPI Spec**: `user-api/src/main/resources/user-ms-api.yaml`
- **Integration Tests**: `user-service/src/test/java/.../integration/`

## License

See main project repository for license information.