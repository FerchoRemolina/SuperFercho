-- Replace identity.users.full_name with first_name + last_name.
-- Existing values are split on the first space; single-token names keep an empty last_name.

ALTER TABLE identity.users
    ADD COLUMN first_name VARCHAR(255),
    ADD COLUMN last_name VARCHAR(255);

UPDATE identity.users
SET
    first_name = CASE
        WHEN position(' ' IN btrim(full_name)) > 0
            THEN left(btrim(full_name), position(' ' IN btrim(full_name)) - 1)
        ELSE btrim(full_name)
    END,
    last_name = CASE
        WHEN position(' ' IN btrim(full_name)) > 0
            THEN btrim(substring(btrim(full_name) FROM position(' ' IN btrim(full_name)) + 1))
        ELSE ''
    END;

ALTER TABLE identity.users
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN last_name SET NOT NULL;

ALTER TABLE identity.users
    DROP COLUMN full_name;
