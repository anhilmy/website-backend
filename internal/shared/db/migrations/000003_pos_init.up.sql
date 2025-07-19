
-- Create menu_categories table
CREATE TABLE IF NOT EXISTS menu_categories (
    category_id SERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    description TEXT
);

-- Create menu_items table
CREATE TABLE IF NOT EXISTS menu_items (
    item_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,
    category_id INT REFERENCES menu_categories(category_id),
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create transactions table
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id),
    total_amount NUMERIC(10,2) NOT NULL,
    payment_method VARCHAR(20) CHECK (payment_method IN ('cash', 'card', 'e-wallet', 'other')) NOT NULL,
    paid_amount NUMERIC(10,2),
    change_due NUMERIC(10,2),
    transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create transaction_items table
CREATE TABLE IF NOT EXISTS transaction_items (
    transaction_item_id SERIAL PRIMARY KEY,
    transaction_id INT REFERENCES transactions(transaction_id),
    item_id INT REFERENCES menu_items(item_id),
    quantity INT NOT NULL,
    item_price NUMERIC(10,2) NOT NULL,
    total_price NUMERIC(10,2) NOT NULL
);
