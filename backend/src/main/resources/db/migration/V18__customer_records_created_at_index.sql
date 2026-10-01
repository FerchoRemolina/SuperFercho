-- Dashboard analytics: period filters over real customer registrations
-- (GET /api/v1/admin/customers/dashboard/new) filter customer_records by created_at.

CREATE INDEX idx_identity_customer_records_created_at
    ON identity.customer_records (created_at);
