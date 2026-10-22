-- Sample menu data for PostgreSQL (run after schema_postgres.sql)
-- Users are seeded by the application (bcrypt hashes) - see DataSeeder.

INSERT INTO menu_categories (name, description, active)
VALUES
    ('Beverages', 'Hot and cold drinks', TRUE),
    ('Main Course', 'Primary dishes', TRUE),
    ('Desserts', 'Sweet dishes', TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Espresso', 'Single shot espresso', 2.50, c.id, TRUE
FROM menu_categories c WHERE c.name = 'Beverages'
ON CONFLICT DO NOTHING;

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Grilled Chicken', 'Chicken breast with vegetables', 12.00, c.id, TRUE
FROM menu_categories c WHERE c.name = 'Main Course'
ON CONFLICT DO NOTHING;

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Chocolate Cake', 'Slice of chocolate cake', 5.00, c.id, TRUE
FROM menu_categories c WHERE c.name = 'Desserts'
ON CONFLICT DO NOTHING;
