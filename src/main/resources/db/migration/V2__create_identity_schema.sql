CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE identity.users (
    id UUID PRIMARY KEY,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_identity_users_email UNIQUE (email),
    CONSTRAINT uk_identity_users_document UNIQUE (document_type, document_number),
    CONSTRAINT ck_identity_users_role CHECK (role IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT ck_identity_users_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE identity.addresses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity.users (id),
    label VARCHAR(100) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    additional_info VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    is_default BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_identity_addresses_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_identity_addresses_user_id ON identity.addresses (user_id);

CREATE UNIQUE INDEX uk_identity_addresses_one_active_default
    ON identity.addresses (user_id)
    WHERE is_default = true AND status = 'ACTIVE';
