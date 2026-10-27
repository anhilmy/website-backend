-- Sample menu data for PostgreSQL (run after schema_postgres.sql)
-- Users are seeded by the application (bcrypt hashes) - see DataSeeder.

INSERT INTO payment_methods
    (code, label, active, is_cash, requires_reference, sort_order)
VALUES
    ('CASH', 'Cash', TRUE, TRUE, FALSE, 10),
    ('CARD', 'Card', TRUE, FALSE, FALSE, 20),
    ('QRIS', 'QRIS', TRUE, FALSE, FALSE, 30),
    ('EWALLET', 'E-Wallet', TRUE, FALSE, FALSE, 40),
    ('TRANSFER', 'Bank Transfer', TRUE, FALSE, TRUE, 50)
ON CONFLICT (code) DO NOTHING;

INSERT INTO charge_templates
    (code, label, calc_type, value, basis, sort_order, taxable, is_tax, active)
VALUES
    ('TAX', 'Tax', 'PERCENTAGE', 0.10, 'SUBTOTAL', 10, FALSE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO menu_categories (name, description, active)
VALUES
    ('Beverages', 'Hot and cold drinks', TRUE),
    ('Main Course', 'Primary dishes', TRUE),
    ('Desserts', 'Sweet dishes', TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Espresso', 'Single shot espresso', 18000.00, c.id, TRUE
FROM menu_categories c WHERE c.name = 'Beverages'
  AND NOT EXISTS (SELECT 1 FROM menu_items m WHERE m.name = 'Espresso');

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Grilled Chicken', 'Chicken breast with vegetables', 45000.00, c.id, TRUE
FROM menu_categories c WHERE c.name = 'Main Course'
  AND NOT EXISTS (SELECT 1 FROM menu_items m WHERE m.name = 'Grilled Chicken');

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Chocolate Cake', 'Slice of chocolate cake', 30000.00, c.id, TRUE
FROM menu_categories c WHERE c.name = 'Desserts'
  AND NOT EXISTS (SELECT 1 FROM menu_items m WHERE m.name = 'Chocolate Cake');
