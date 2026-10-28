-- V9 (PostgreSQL): convert money columns to integer minor units.
-- Apply with: psql -U <user> -d <db> -f V9__money_minor_units_postgres.sql
--
-- For databases created before V9 only. A fresh baseline already stores scaled BIGINT values,
-- so running this script twice would double every amount.
--
-- Money uses scale 2 (100 minor units per rupiah); charge/rate values use scale 6 so both
-- percentage rates and fixed amounts stay exact.

ALTER TABLE menu_items
    ALTER COLUMN price TYPE BIGINT USING ROUND(price * 100)::BIGINT;

ALTER TABLE transactions
    ALTER COLUMN total_amount TYPE BIGINT USING ROUND(total_amount * 100)::BIGINT,
    ALTER COLUMN subtotal_amount TYPE BIGINT USING ROUND(subtotal_amount * 100)::BIGINT,
    ALTER COLUMN charges_amount TYPE BIGINT USING ROUND(charges_amount * 100)::BIGINT,
    ALTER COLUMN tax_amount TYPE BIGINT USING ROUND(tax_amount * 100)::BIGINT,
    ALTER COLUMN payment_amount TYPE BIGINT USING ROUND(payment_amount * 100)::BIGINT,
    ALTER COLUMN refunded_amount TYPE BIGINT USING ROUND(refunded_amount * 100)::BIGINT;

ALTER TABLE transaction_items
    ALTER COLUMN unit_price TYPE BIGINT USING ROUND(unit_price * 100)::BIGINT,
    ALTER COLUMN total_price TYPE BIGINT USING ROUND(total_price * 100)::BIGINT;

ALTER TABLE modifier_options
    ALTER COLUMN price_delta TYPE BIGINT USING ROUND(price_delta * 100)::BIGINT;

ALTER TABLE transaction_item_modifiers
    ALTER COLUMN price_delta TYPE BIGINT USING ROUND(price_delta * 100)::BIGINT;

ALTER TABLE refunds
    ALTER COLUMN amount TYPE BIGINT USING ROUND(amount * 100)::BIGINT;

ALTER TABLE charge_templates
    ALTER COLUMN value TYPE BIGINT USING ROUND(value * 1000000)::BIGINT;

ALTER TABLE charges
    ALTER COLUMN value TYPE BIGINT USING ROUND(value * 1000000)::BIGINT,
    ALTER COLUMN computed_amount TYPE BIGINT USING ROUND(computed_amount * 100)::BIGINT;

INSERT INTO schema_migrations (version, description)
VALUES ('V9', 'money minor units (postgres)')
ON CONFLICT (version) DO NOTHING;
