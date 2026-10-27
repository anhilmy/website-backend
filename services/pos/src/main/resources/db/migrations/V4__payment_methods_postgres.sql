-- V4 (PostgreSQL): payment methods + payment reference on the bill.
-- Apply with: psql -U <user> -d <db> -f V4__payment_methods_postgres.sql

CREATE TABLE IF NOT EXISTS payment_methods (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    label VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_cash BOOLEAN NOT NULL DEFAULT FALSE,
    requires_reference BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_payment_methods_active ON payment_methods (active);

INSERT INTO schema_migrations (version, description)
VALUES ('V4', 'payment methods (postgres)')
ON CONFLICT (version) DO NOTHING;
