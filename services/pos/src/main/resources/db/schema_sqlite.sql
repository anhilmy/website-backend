-- SQLite schema for POS system
-- Applied by: sqlite3 pos_dev.db < schema_sqlite.sql

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS menu_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    active INTEGER NOT NULL DEFAULT 1,
    parent_id INTEGER,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES menu_categories(id)
);

CREATE TABLE IF NOT EXISTS menu_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    price REAL NOT NULL,
    category_id INTEGER NOT NULL,
    available INTEGER NOT NULL DEFAULT 1,
    image_url TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES menu_categories(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    version INTEGER NOT NULL DEFAULT 0,
    total_amount REAL NOT NULL DEFAULT 0,
    subtotal_amount REAL NOT NULL DEFAULT 0,
    charges_amount REAL NOT NULL DEFAULT 0,
    tax_amount REAL NOT NULL DEFAULT 0,
    payment_method TEXT,
    payment_amount REAL,
    payment_reference TEXT,
    refunded_amount REAL NOT NULL DEFAULT 0,
    amended_from_id INTEGER,
    amended_to_id INTEGER,
    status TEXT NOT NULL DEFAULT 'DRAFT',
    customer_info TEXT,
    cashier_id INTEGER NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cashier_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS transaction_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_id INTEGER NOT NULL,
    menu_item_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price REAL NOT NULL,
    total_price REAL NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    void_reason TEXT,
    voided_by TEXT,
    voided_at DATETIME,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
);

CREATE TABLE IF NOT EXISTS refunds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_id INTEGER NOT NULL,
    transaction_item_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    amount REAL NOT NULL DEFAULT 0,
    reason TEXT NOT NULL,
    payment_method TEXT,
    created_by TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_item_id) REFERENCES transaction_items(id)
);

CREATE TABLE IF NOT EXISTS payment_methods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,
    label TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    is_cash INTEGER NOT NULL DEFAULT 0,
    requires_reference INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS charge_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,
    label TEXT NOT NULL,
    calc_type TEXT NOT NULL DEFAULT 'PERCENTAGE',
    value REAL NOT NULL DEFAULT 0,
    basis TEXT NOT NULL DEFAULT 'SUBTOTAL',
    sort_order INTEGER NOT NULL DEFAULT 0,
    taxable INTEGER NOT NULL DEFAULT 0,
    is_tax INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS charges (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_id INTEGER NOT NULL,
    code TEXT NOT NULL,
    label TEXT NOT NULL,
    calc_type TEXT NOT NULL DEFAULT 'PERCENTAGE',
    value REAL NOT NULL DEFAULT 0,
    basis TEXT NOT NULL DEFAULT 'SUBTOTAL',
    sort_order INTEGER NOT NULL DEFAULT 0,
    taxable INTEGER NOT NULL DEFAULT 0,
    is_tax INTEGER NOT NULL DEFAULT 0,
    computed_amount REAL NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS dining_tables (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    area TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'AVAILABLE',
    current_transaction_id INTEGER,
    joined_to_id INTEGER,
    version INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
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
