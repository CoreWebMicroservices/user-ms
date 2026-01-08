-- ============================================================================
-- V1.0.0 - Create user_ms schema
-- ============================================================================
-- Based on: UserEntity, RoleEntity, LoginTokenEntity, VerificationTokenEntity
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS user_ms;

COMMENT ON SCHEMA user_ms IS 'User management service';

SET search_path TO user_ms;

-- ----------------------------------------------------------------------------
-- app_user table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_user (
    id              SERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE,
    provider        VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    first_name      VARCHAR(50),
    last_name       VARCHAR(50),
    image_url       VARCHAR(255),
    phone_number    VARCHAR(50),
    password        VARCHAR(255),
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified  BOOLEAN DEFAULT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at   TIMESTAMP WITH TIME ZONE
);

-- Create sequence for app_user (required by GenerationType.AUTO)
CREATE SEQUENCE IF NOT EXISTS app_user_seq INCREMENT BY 1 OWNED BY app_user.id;
ALTER TABLE app_user ALTER COLUMN id SET DEFAULT nextval('app_user_seq');

CREATE INDEX IF NOT EXISTS idx_app_user_email ON app_user(email);
CREATE INDEX IF NOT EXISTS idx_app_user_uuid ON app_user(uuid);
CREATE INDEX IF NOT EXISTS idx_app_user_created_at ON app_user(created_at);

-- ----------------------------------------------------------------------------
-- app_user_role table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_user_role (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER NOT NULL,
    name            VARCHAR(36) NOT NULL,
    updated_by      INTEGER,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- Create sequence for app_user_role (required by GenerationType.AUTO)
CREATE SEQUENCE IF NOT EXISTS app_user_role_seq INCREMENT BY 1 OWNED BY app_user_role.id;
ALTER TABLE app_user_role ALTER COLUMN id SET DEFAULT nextval('app_user_role_seq');

CREATE INDEX IF NOT EXISTS idx_app_user_role_user ON app_user_role(user_id);

-- ----------------------------------------------------------------------------
-- login_token table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS login_token (
    id              SERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE,
    user_id         INTEGER NOT NULL,
    token           TEXT NOT NULL UNIQUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_login_token_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- Create sequence for login_token (required by GenerationType.AUTO)
CREATE SEQUENCE IF NOT EXISTS login_token_seq INCREMENT BY 1 OWNED BY login_token.id;
ALTER TABLE login_token ALTER COLUMN id SET DEFAULT nextval('login_token_seq');

CREATE INDEX IF NOT EXISTS idx_login_token_user ON login_token(user_id);
CREATE INDEX IF NOT EXISTS idx_login_token_uuid ON login_token(uuid);
CREATE INDEX IF NOT EXISTS idx_login_token_created_at ON login_token(created_at);

-- ----------------------------------------------------------------------------
-- verification_token table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS verification_token (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE,
    user_id         INTEGER NOT NULL,
    token           VARCHAR(255) NOT NULL,
    type            VARCHAR(10) NOT NULL CHECK (type IN ('EMAIL', 'SMS')),
    expires_at      TIMESTAMP NOT NULL,
    is_used         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_verification_token_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- Create sequence for verification_token
CREATE SEQUENCE IF NOT EXISTS verification_token_seq INCREMENT BY 1 OWNED BY verification_token.id;
ALTER TABLE verification_token ALTER COLUMN id SET DEFAULT nextval('verification_token_seq');

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_verification_token_token_type ON verification_token(token, type);
CREATE INDEX IF NOT EXISTS idx_verification_token_user_type ON verification_token(user_id, type);
CREATE INDEX IF NOT EXISTS idx_verification_token_expires_at ON verification_token(expires_at);
CREATE INDEX IF NOT EXISTS idx_verification_token_uuid ON verification_token(uuid);

RESET search_path;
