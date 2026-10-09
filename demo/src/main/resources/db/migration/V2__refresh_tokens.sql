CREATE TABLE refresh_tokens
(
    id         UUID PRIMARY KEY             DEFAULT gen_random_uuid(),
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    user_id    UUID                NOT NULL REFERENCES users (id),
    expires_at TIMESTAMPTZ         NOT NULL,
    revoked    BOOLEAN             NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ         NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);