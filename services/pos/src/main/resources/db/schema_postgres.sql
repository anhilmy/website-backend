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
    total_amount NUMERIC(38,2) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(38,2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(255),
    payment_amount NUMERIC(38,2),
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
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
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
CREATE INDEX IF NOT EXISTS idx_menu_categories_active ON menu_categories (active);
CREATE INDEX IF NOT EXISTS idx_menu_items_category ON menu_items (category_id);
CREATE INDEX IF NOT EXISTS idx_menu_items_available ON menu_items (available);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_transactions_cashier ON transactions (cashier_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions (created_at);
CREATE INDEX IF NOT EXISTS idx_transaction_items_transaction ON transaction_items (transaction_id);
