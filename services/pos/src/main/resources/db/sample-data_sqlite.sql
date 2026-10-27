-- Sample menu data for SQLite (run after schema_sqlite.sql)
-- Users are seeded by the application (bcrypt hashes) - see DataSeeder.

INSERT OR IGNORE INTO payment_methods
    (code, label, active, is_cash, requires_reference, sort_order)
VALUES
    ('CASH', 'Cash', 1, 1, 0, 10),
    ('CARD', 'Card', 1, 0, 0, 20),
    ('QRIS', 'QRIS', 1, 0, 0, 30),
    ('EWALLET', 'E-Wallet', 1, 0, 0, 40),
    ('TRANSFER', 'Bank Transfer', 1, 0, 1, 50);

INSERT OR IGNORE INTO charge_templates
    (code, label, calc_type, value, basis, sort_order, taxable, is_tax, active)
VALUES
    ('TAX', 'Tax', 'PERCENTAGE', 0.10, 'SUBTOTAL', 10, 0, 1, 1);

INSERT OR IGNORE INTO menu_categories (name, description, active)
VALUES
    ('Beverages', 'Hot and cold drinks', 1),
    ('Main Course', 'Primary dishes', 1),
    ('Desserts', 'Sweet dishes', 1);

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Espresso', 'Single shot espresso', 18000.00, id, 1
FROM menu_categories WHERE name = 'Beverages'
  AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Espresso');

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Grilled Chicken', 'Chicken breast with vegetables', 45000.00, id, 1
FROM menu_categories WHERE name = 'Main Course'
  AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Grilled Chicken');

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Chocolate Cake', 'Slice of chocolate cake', 30000.00, id, 1
FROM menu_categories WHERE name = 'Desserts'
  AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Chocolate Cake');
