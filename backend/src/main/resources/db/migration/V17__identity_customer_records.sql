-- CustomerRecord: commercial identity separate from operational User accounts.

CREATE TABLE identity.customer_records (
    id UUID PRIMARY KEY,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    billing_first_name VARCHAR(255) NOT NULL,
    billing_last_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_identity_customer_records_document UNIQUE (document_type, document_number),
    CONSTRAINT ck_identity_customer_records_timestamps CHECK (created_at <= updated_at)
);

CREATE INDEX idx_identity_customer_records_document
    ON identity.customer_records (document_type, document_number);

ALTER TABLE identity.users
    ADD COLUMN customer_record_id UUID,
    ADD COLUMN deleted_at TIMESTAMPTZ;

-- One CustomerRecord per commercial CUSTOMER (exclude storefront-preview temporary users).
INSERT INTO identity.customer_records (
    id,
    document_type,
    document_number,
    billing_first_name,
    billing_last_name,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    u.document_type,
    u.document_number,
    u.first_name,
    CASE
        WHEN btrim(u.last_name) = '' THEN u.first_name
        ELSE u.last_name
    END,
    u.created_at,
    u.updated_at
FROM identity.users u
WHERE u.role = 'CUSTOMER'
  AND u.document_number NOT LIKE 'PREV%'
  AND u.email NOT LIKE 'preview+%@temp.superfercho.local';

UPDATE identity.users u
SET customer_record_id = cr.id
FROM identity.customer_records cr
WHERE u.role = 'CUSTOMER'
  AND u.document_type = cr.document_type
  AND u.document_number = cr.document_number
  AND u.document_number NOT LIKE 'PREV%'
  AND u.email NOT LIKE 'preview+%@temp.superfercho.local';

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM identity.users u
        WHERE u.role = 'CUSTOMER'
          AND u.customer_record_id IS NULL
          AND u.document_number NOT LIKE 'PREV%'
          AND u.email NOT LIKE 'preview+%@temp.superfercho.local'
    ) THEN
        RAISE EXCEPTION
            'V17 backfill incomplete: commercial CUSTOMER without customer_record_id';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM identity.users u
        WHERE u.role = 'ADMIN'
          AND u.customer_record_id IS NOT NULL
    ) THEN
        RAISE EXCEPTION 'V17 backfill invalid: ADMIN must not have customer_record_id';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM identity.users u
        WHERE u.role = 'CUSTOMER'
          AND u.customer_record_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1
              FROM identity.customer_records cr
              WHERE cr.id = u.customer_record_id
                AND cr.document_type = u.document_type
                AND cr.document_number = u.document_number
          )
    ) THEN
        RAISE EXCEPTION
            'V17 backfill invalid: CUSTOMER customer_record_id does not match document';
    END IF;
END $$;

ALTER TABLE identity.users
    ALTER COLUMN document_type DROP NOT NULL,
    ALTER COLUMN document_number DROP NOT NULL;

ALTER TABLE identity.users
    DROP CONSTRAINT uk_identity_users_document;

ALTER TABLE identity.users
    DROP CONSTRAINT uk_identity_users_email;

CREATE UNIQUE INDEX uk_identity_users_email
    ON identity.users (email)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_identity_users_one_live_customer_record
    ON identity.users (customer_record_id)
    WHERE deleted_at IS NULL
      AND customer_record_id IS NOT NULL;

ALTER TABLE identity.users
    ADD CONSTRAINT fk_identity_users_customer_record
        FOREIGN KEY (customer_record_id) REFERENCES identity.customer_records (id);
