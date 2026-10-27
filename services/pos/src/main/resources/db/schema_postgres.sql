-- PostgreSQL schema for POS system
-- Applied by: psql -U <user> -d <db> -f schema_postgres.sql
-- Column types MUST match the JPA entities (Long -> BIGINT, String -> VARCHAR/TEXT,
-- BigDecimal -> NUMERIC, LocalDateTime -> TIMESTAMP) because docker/prod run with
-- hibernate.hbm2ddl.auto=validate.

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS menu_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    parent_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES menu_categories(id)
);

CREATE TABLE IF NOT EXISTS menu_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    price NUMERIC(38,2) NOT NULL,
    category_id BIGINT NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    image_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES menu_categories(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    total_amount NUMERIC(38,2) NOT NULL DEFAULT 0,
    subtotal_amount NUMERIC(38,2) NOT NULL DEFAULT 0,
    charges_amount NUMERIC(38,2) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(38,2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(255),
    payment_amount NUMERIC(38,2),
    payment_reference VARCHAR(255),
    refunded_amount NUMERIC(38,2) NOT NULL DEFAULT 0,
    amended_from_id BIGINT,
    amended_to_id BIGINT,
    status VARCHAR(255) NOT NULL DEFAULT 'DRAFT',
    customer_info VARCHAR(255),
    cashier_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cashier_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS transaction_items (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(38,2) NOT NULL,
    total_price NUMERIC(38,2) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    void_reason VARCHAR(255),
    voided_by VARCHAR(255),
    voided_at TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
);

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

CREATE TABLE IF NOT EXISTS dining_tables (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    area VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'AVAILABLE',
    current_transaction_id BIGINT,
    joined_to_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (current_transaction_id) REFERENCES transactions(id),
    FOREIGN KEY (joined_to_id) REFERENCES dining_tables(id)
);

-- Indexes for common lookups
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_dining_tables_status ON dining_tables (status);
CREATE INDEX IF NOT EXISTS idx_dining_tables_area ON dining_tables (area);
-- A bill can be seated at most one table (NULLs are distinct, so free tables are unaffected).
CREATE UNIQUE INDEX IF NOT EXISTS uq_dining_tables_bill ON dining_tables (current_transaction_id);
-- A cancelled bill may be amended at most once.
CREATE UNIQUE INDEX IF NOT EXISTS uq_transactions_amended_from
    ON transactions (amended_from_id) WHERE amended_from_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_menu_categories_active ON menu_categories (active);
CREATE INDEX IF NOT EXISTS idx_menu_items_category ON menu_items (category_id);
CREATE INDEX IF NOT EXISTS idx_menu_items_available ON menu_items (available);
CREATE INDEX IF NOT EXISTS idx_payment_methods_active ON payment_methods (active);
CREATE INDEX IF NOT EXISTS idx_charge_templates_active ON charge_templates (active);
CREATE INDEX IF NOT EXISTS idx_charges_transaction ON charges (transaction_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_transactions_cashier ON transactions (cashier_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions (created_at);
CREATE INDEX IF NOT EXISTS idx_transaction_items_transaction ON transaction_items (transaction_id);
CREATE INDEX IF NOT EXISTS idx_refunds_transaction ON refunds (transaction_id);
CREATE INDEX IF NOT EXISTS idx_refunds_item ON refunds (transaction_item_id);
