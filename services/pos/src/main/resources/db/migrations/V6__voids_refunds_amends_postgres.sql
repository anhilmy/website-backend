-- V6 (PostgreSQL): line voids, append-only refunds and amendment links.
-- Apply with: psql -U <user> -d <db> -f V6__voids_refunds_amends_postgres.sql

ALTER TABLE transaction_items ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE transaction_items ADD COLUMN IF NOT EXISTS void_reason VARCHAR(255);
ALTER TABLE transaction_items ADD COLUMN IF NOT EXISTS voided_by VARCHAR(255);
ALTER TABLE transaction_items ADD COLUMN IF NOT EXISTS voided_at TIMESTAMP;

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS refunded_amount NUMERIC(38,2) NOT NULL DEFAULT 0;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS amended_from_id BIGINT;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS amended_to_id BIGINT;

CREATE TABLE IF NOT EXISTS refunds (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    transaction_item_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    amount NUMERIC(38,2) NOT NULL DEFAULT 0,
    reason VARCHAR(255) NOT NULL,
    payment_method VARCHAR(32),
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_item_id) REFERENCES transaction_items(id)
);

CREATE INDEX IF NOT EXISTS idx_refunds_transaction ON refunds (transaction_id);
CREATE INDEX IF NOT EXISTS idx_refunds_item ON refunds (transaction_item_id);

INSERT INTO schema_migrations (version, description)
VALUES ('V6', 'voids, refunds, amends (postgres)')
ON CONFLICT (version) DO NOTHING;
