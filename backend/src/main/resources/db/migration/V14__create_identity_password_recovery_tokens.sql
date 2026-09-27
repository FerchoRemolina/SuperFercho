CREATE TABLE identity.password_recovery_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity.users (id),
    token_hash VARCHAR(64) NOT NULL,
    request_ip VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CONSTRAINT uk_identity_password_recovery_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_identity_password_recovery_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_identity_password_recovery_tokens_user_created
    ON identity.password_recovery_tokens (user_id, created_at DESC);

CREATE INDEX idx_identity_password_recovery_tokens_user_active
    ON identity.password_recovery_tokens (user_id)
    WHERE consumed_at IS NULL;
