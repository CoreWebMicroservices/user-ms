-- ============================================================================
-- V1.0.0 - Create user_ms schema
-- ============================================================================
-- Based on: UserEntity, RoleEntity, LoginTokenEntity, ActionTokenEntity, AuthorizationCodeEntity
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS user_ms;

COMMENT ON SCHEMA user_ms IS 'User management service';

SET search_path TO user_ms;

-- ----------------------------------------------------------------------------
-- app_user table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_user (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE,
    provider        VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    first_name      VARCHAR(50),
    last_name       VARCHAR(50),
    image_url       VARCHAR(255),
    phone_number    VARCHAR(50) UNIQUE,
    password        VARCHAR(255),
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified  BOOLEAN DEFAULT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_app_user_email ON app_user(email);
CREATE INDEX IF NOT EXISTS idx_app_user_uuid ON app_user(uuid);
CREATE INDEX IF NOT EXISTS idx_app_user_created_at ON app_user(created_at);

-- ----------------------------------------------------------------------------
-- app_user_role table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_user_role (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(36) NOT NULL,
    updated_by      BIGINT,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_app_user_role_user ON app_user_role(user_id);

-- ----------------------------------------------------------------------------
-- login_token table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS login_token (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL,
    token           TEXT NOT NULL UNIQUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_login_token_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_login_token_user ON login_token(user_id);
CREATE INDEX IF NOT EXISTS idx_login_token_uuid ON login_token(uuid);
CREATE INDEX IF NOT EXISTS idx_login_token_created_at ON login_token(created_at);

-- ----------------------------------------------------------------------------
-- action_tokens table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS action_tokens (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE,
    token_hash      VARCHAR(64) NOT NULL UNIQUE,
    action_type     VARCHAR(50) NOT NULL,
    user_id         BIGINT NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    used            BOOLEAN NOT NULL DEFAULT FALSE,
    used_at         TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_action_tokens_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- Essential indexes only
CREATE INDEX IF NOT EXISTS idx_action_tokens_token_hash ON action_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_action_tokens_user_id ON action_tokens(user_id);

-- ----------------------------------------------------------------------------
-- authorization_codes table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS authorization_codes (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(255) NOT NULL UNIQUE,
    client_id       VARCHAR(255) NOT NULL,
    redirect_uri    VARCHAR(500) NOT NULL,
    user_id         BIGINT NOT NULL,
    scope           VARCHAR(500),
    code_challenge  VARCHAR(255),
    code_challenge_method VARCHAR(10),
    nonce           VARCHAR(255),
    state           VARCHAR(255),
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used         BOOLEAN NOT NULL DEFAULT FALSE,
    used_at         TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_authorization_codes_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_authorization_codes_code ON authorization_codes(code);
CREATE INDEX IF NOT EXISTS idx_authorization_codes_user_id ON authorization_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_authorization_codes_expires_at ON authorization_codes(expires_at);

RESET search_path;
