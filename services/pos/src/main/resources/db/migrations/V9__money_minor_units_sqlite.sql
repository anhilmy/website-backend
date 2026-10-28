-- V9 (SQLite): convert money columns to integer minor units.
-- Apply with: sqlite3 <db> < V9__money_minor_units_sqlite.sql
--
-- For databases created before V9 only. A fresh baseline already stores scaled BIGINT values,
-- so running this script twice would double every amount.
--
-- Money uses scale 2 (100 minor units per rupiah); charge/rate values use scale 6 so both
-- percentage rates and fixed amounts stay exact.

UPDATE menu_items SET price = CAST(ROUND(price * 100) AS INTEGER);

UPDATE transactions SET
    total_amount = CAST(ROUND(total_amount * 100) AS INTEGER),
    subtotal_amount = CAST(ROUND(subtotal_amount * 100) AS INTEGER),
    charges_amount = CAST(ROUND(charges_amount * 100) AS INTEGER),
    tax_amount = CAST(ROUND(tax_amount * 100) AS INTEGER),
    payment_amount = CASE WHEN payment_amount IS NULL THEN NULL
        ELSE CAST(ROUND(payment_amount * 100) AS INTEGER) END,
    refunded_amount = CAST(ROUND(refunded_amount * 100) AS INTEGER);

UPDATE transaction_items SET
    unit_price = CAST(ROUND(unit_price * 100) AS INTEGER),
    total_price = CAST(ROUND(total_price * 100) AS INTEGER);

UPDATE modifier_options SET price_delta = CAST(ROUND(price_delta * 100) AS INTEGER);

UPDATE transaction_item_modifiers SET price_delta = CAST(ROUND(price_delta * 100) AS INTEGER);

UPDATE refunds SET amount = CAST(ROUND(amount * 100) AS INTEGER);

UPDATE charge_templates SET value = CAST(ROUND(value * 1000000) AS INTEGER);

UPDATE charges SET
    value = CAST(ROUND(value * 1000000) AS INTEGER),
    computed_amount = CAST(ROUND(computed_amount * 100) AS INTEGER);

INSERT INTO schema_migrations (version, description)
VALUES ('V9', 'money minor units (sqlite)')
ON CONFLICT (version) DO NOTHING;
