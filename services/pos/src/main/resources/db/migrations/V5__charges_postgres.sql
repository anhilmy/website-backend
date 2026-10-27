-- V5 (PostgreSQL): charge templates + order charge snapshots + persisted subtotal.
-- Apply with: psql -U <user> -d <db> -f V5__charges_postgres.sql

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS subtotal_amount NUMERIC(38,2) NOT NULL DEFAULT 0;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS charges_amount NUMERIC(38,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS charge_templates (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    label VARCHAR(64) NOT NULL,
    calc_type VARCHAR(16) NOT NULL DEFAULT 'PERCENTAGE',
    value NUMERIC(38,2) NOT NULL DEFAULT 0,
    basis VARCHAR(16) NOT NULL DEFAULT 'SUBTOTAL',
    sort_order INTEGER NOT NULL DEFAULT 0,
    taxable BOOLEAN NOT NULL DEFAULT FALSE,
    is_tax BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS charges (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    code VARCHAR(32) NOT NULL,
    label VARCHAR(64) NOT NULL,
    calc_type VARCHAR(16) NOT NULL DEFAULT 'PERCENTAGE',
    value NUMERIC(38,2) NOT NULL DEFAULT 0,
    basis VARCHAR(16) NOT NULL DEFAULT 'SUBTOTAL',
    sort_order INTEGER NOT NULL DEFAULT 0,
    taxable BOOLEAN NOT NULL DEFAULT FALSE,
    is_tax BOOLEAN NOT NULL DEFAULT FALSE,
    computed_amount NUMERIC(38,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_charge_templates_active ON charge_templates (active);
CREATE INDEX IF NOT EXISTS idx_charges_transaction ON charges (transaction_id);

INSERT INTO charge_templates
    (code, label, calc_type, value, basis, sort_order, taxable, is_tax, active)
VALUES ('TAX', 'Tax', 'PERCENTAGE', 0.10, 'SUBTOTAL', 10, FALSE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO schema_migrations (version, description)
VALUES ('V5', 'charges (postgres)')
ON CONFLICT (version) DO NOTHING;
