-- Sample menu data for PostgreSQL (run after schema_postgres.sql)
-- Users are seeded by the application (bcrypt hashes) - see DataSeeder.
-- Money values are minor units (scale 2): Rp 18.000 -> 1800000.
-- Charge values are scale 6: 0.10 (10%) -> 100000.

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
    ('TAX', 'Tax', 'PERCENTAGE', 100000, 'SUBTOTAL', 10, FALSE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO menu_categories (name, description, active)
VALUES
    ('Beverages', 'Hot and cold drinks', TRUE),
    ('Main Course', 'Primary dishes', TRUE),
    ('Desserts', 'Sweet dishes', TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Espresso', 'Single shot espresso', 1800000, c.id, TRUE
FROM menu_categories c WHERE c.name = 'Beverages'
  AND NOT EXISTS (SELECT 1 FROM menu_items m WHERE m.name = 'Espresso');

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Grilled Chicken', 'Chicken breast with vegetables', 4500000, c.id, TRUE
FROM menu_categories c WHERE c.name = 'Main Course'
  AND NOT EXISTS (SELECT 1 FROM menu_items m WHERE m.name = 'Grilled Chicken');

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Chocolate Cake', 'Slice of chocolate cake', 3000000, c.id, TRUE
FROM menu_categories c WHERE c.name = 'Desserts'
  AND NOT EXISTS (SELECT 1 FROM menu_items m WHERE m.name = 'Chocolate Cake');

-- Espresso modifiers: a required temperature choice and optional extras.
INSERT INTO modifier_groups
    (menu_item_id, name, selection_type, required, min_select, max_select, sort_order, active)
SELECT i.id, 'Temperature', 'SINGLE', TRUE, 1, 1, 10, TRUE
FROM menu_items i WHERE i.name = 'Espresso'
  AND NOT EXISTS (
      SELECT 1 FROM modifier_groups g
      JOIN menu_items m ON g.menu_item_id = m.id
      WHERE m.name = 'Espresso' AND g.name = 'Temperature');

INSERT INTO modifier_groups
    (menu_item_id, name, selection_type, required, min_select, max_select, sort_order, active)
SELECT i.id, 'Extras', 'MULTI', FALSE, 0, 3, 20, TRUE
FROM menu_items i WHERE i.name = 'Espresso'
  AND NOT EXISTS (
      SELECT 1 FROM modifier_groups g
      JOIN menu_items m ON g.menu_item_id = m.id
      WHERE m.name = 'Espresso' AND g.name = 'Extras');

INSERT INTO modifier_options
    (group_id, name, price_delta, is_default, available, sort_order)
SELECT g.id, 'Hot', 0, TRUE, TRUE, 10
FROM modifier_groups g JOIN menu_items m ON g.menu_item_id = m.id
WHERE m.name = 'Espresso' AND g.name = 'Temperature'
  AND NOT EXISTS (
      SELECT 1 FROM modifier_options o WHERE o.group_id = g.id AND o.name = 'Hot');

INSERT INTO modifier_options
    (group_id, name, price_delta, is_default, available, sort_order)
SELECT g.id, 'Iced', 0, FALSE, TRUE, 20
FROM modifier_groups g JOIN menu_items m ON g.menu_item_id = m.id
WHERE m.name = 'Espresso' AND g.name = 'Temperature'
  AND NOT EXISTS (
      SELECT 1 FROM modifier_options o WHERE o.group_id = g.id AND o.name = 'Iced');

INSERT INTO modifier_options
    (group_id, name, price_delta, is_default, available, sort_order)
SELECT g.id, 'Extra shot', 500000, FALSE, TRUE, 10
FROM modifier_groups g JOIN menu_items m ON g.menu_item_id = m.id
WHERE m.name = 'Espresso' AND g.name = 'Extras'
  AND NOT EXISTS (
      SELECT 1 FROM modifier_options o WHERE o.group_id = g.id AND o.name = 'Extra shot');

INSERT INTO modifier_options
    (group_id, name, price_delta, is_default, available, sort_order)
SELECT g.id, 'Oat milk', 800000, FALSE, TRUE, 20
FROM modifier_groups g JOIN menu_items m ON g.menu_item_id = m.id
WHERE m.name = 'Espresso' AND g.name = 'Extras'
  AND NOT EXISTS (
      SELECT 1 FROM modifier_options o WHERE o.group_id = g.id AND o.name = 'Oat milk');
