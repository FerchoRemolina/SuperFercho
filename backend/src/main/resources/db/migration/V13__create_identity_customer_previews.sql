CREATE TABLE identity.customer_previews (
    id UUID PRIMARY KEY,
    admin_user_id UUID NOT NULL,
    temporary_customer_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    CONSTRAINT ck_identity_customer_previews_status CHECK (status IN ('ACTIVE', 'CLOSED')),
    CONSTRAINT ck_identity_customer_previews_expiry CHECK (expires_at > created_at)
);

CREATE UNIQUE INDEX uk_identity_customer_previews_one_active_admin
    ON identity.customer_previews (admin_user_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uk_identity_customer_previews_temporary_customer_id
    ON identity.customer_previews (temporary_customer_id);

CREATE INDEX idx_identity_customer_previews_admin_user_id
    ON identity.customer_previews (admin_user_id);

CREATE INDEX idx_identity_customer_previews_status_expires_at
    ON identity.customer_previews (status, expires_at);
