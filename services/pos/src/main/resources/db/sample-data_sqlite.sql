-- Sample menu data for SQLite (run after schema_sqlite.sql)
-- Users are seeded by the application (bcrypt hashes) - see DataSeeder.

INSERT OR IGNORE INTO menu_categories (name, description, active)
VALUES
    ('Beverages', 'Hot and cold drinks', 1),
    ('Main Course', 'Primary dishes', 1),
    ('Desserts', 'Sweet dishes', 1);

INSERT OR IGNORE INTO menu_items (name, description, price, category_id, available)
SELECT 'Espresso', 'Single shot espresso', 2.50, id, 1
FROM menu_categories WHERE name = 'Beverages';

INSERT OR IGNORE INTO menu_items (name, description, price, category_id, available)
SELECT 'Grilled Chicken', 'Chicken breast with vegetables', 12.00, id, 1
FROM menu_categories WHERE name = 'Main Course';

INSERT OR IGNORE INTO menu_items (name, description, price, category_id, available)
SELECT 'Chocolate Cake', 'Slice of chocolate cake', 5.00, id, 1
FROM menu_categories WHERE name = 'Desserts';
