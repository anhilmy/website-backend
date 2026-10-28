-- Sample menu data for SQLite (run after schema_sqlite.sql)
-- Users are seeded by the application (bcrypt hashes) - see DataSeeder.
-- Money values are minor units (scale 2): Rp 18.000 -> 1800000.
-- Charge values are scale 6: 0.10 (10%) -> 100000.

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
    ('TAX', 'Tax', 'PERCENTAGE', 100000, 'SUBTOTAL', 10, 0, 1, 1);

INSERT OR IGNORE INTO menu_categories (name, description, active)
VALUES
    ('Beverages', 'Hot and cold drinks', 1),
    ('Main Course', 'Primary dishes', 1),
    ('Desserts', 'Sweet dishes', 1);

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Espresso', 'Single shot espresso', 1800000, id, 1
FROM menu_categories WHERE name = 'Beverages'
  AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Espresso');

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Grilled Chicken', 'Chicken breast with vegetables', 4500000, id, 1
FROM menu_categories WHERE name = 'Main Course'
  AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Grilled Chicken');

INSERT INTO menu_items (name, description, price, category_id, available)
SELECT 'Chocolate Cake', 'Slice of chocolate cake', 3000000, id, 1
FROM menu_categories WHERE name = 'Desserts'
  AND NOT EXISTS (SELECT 1 FROM menu_items WHERE name = 'Chocolate Cake');

-- Espresso modifiers: a required temperature choice and optional extras.
INSERT INTO modifier_groups
    (menu_item_id, name, selection_type, required, min_select, max_select, sort_order, active)
SELECT id, 'Temperature', 'SINGLE', 1, 1, 1, 10, 1
FROM menu_items WHERE name = 'Espresso'
  AND NOT EXISTS (
      SELECT 1 FROM modifier_groups g
      JOIN menu_items m ON g.menu_item_id = m.id
      WHERE m.name = 'Espresso' AND g.name = 'Temperature');

INSERT INTO modifier_groups
    (menu_item_id, name, selection_type, required, min_select, max_select, sort_order, active)
SELECT id, 'Extras', 'MULTI', 0, 0, 3, 20, 1
FROM menu_items WHERE name = 'Espresso'
  AND NOT EXISTS (
      SELECT 1 FROM modifier_groups g
      JOIN menu_items m ON g.menu_item_id = m.id
      WHERE m.name = 'Espresso' AND g.name = 'Extras');

INSERT INTO modifier_options
    (group_id, name, price_delta, is_default, available, sort_order)
SELECT g.id, 'Hot', 0, 1, 1, 10
FROM modifier_groups g JOIN menu_items m ON g.menu_item_id = m.id
WHERE m.name = 'Espresso' AND g.name = 'Temperature'
  AND NOT EXISTS (
      SELECT 1 FROM modifier_options o WHERE o.group_id = g.id AND o.name = 'Hot');

INSERT INTO modifier_options
    (group_id, name, price_delta, is_default, available, sort_order)
SELECT g.id, 'Iced', 0, 0, 1, 20
FROM modifier_groups g JOIN menu_items m ON g.menu_item_id = m.id
WHERE m.name = 'Espresso' AND g.name = 'Temperature'
  AND NOT EXISTS (
      SELECT 1 FROM modifier_options o WHERE o.group_id = g.id AND o.name = 'Iced');

INSERT INTO modifier_options
    (group_id, name, price_delta, is_default, available, sort_order)
SELECT g.id, 'Extra shot', 500000, 0, 1, 10
FROM modifier_groups g JOIN menu_items m ON g.menu_item_id = m.id
WHERE m.name = 'Espresso' AND g.name = 'Extras'
  AND NOT EXISTS (
      SELECT 1 FROM modifier_options o WHERE o.group_id = g.id AND o.name = 'Extra shot');

INSERT INTO modifier_options
    (group_id, name, price_delta, is_default, available, sort_order)
SELECT g.id, 'Oat milk', 800000, 0, 1, 20
FROM modifier_groups g JOIN menu_items m ON g.menu_item_id = m.id
WHERE m.name = 'Espresso' AND g.name = 'Extras'
  AND NOT EXISTS (
      SELECT 1 FROM modifier_options o WHERE o.group_id = g.id AND o.name = 'Oat milk');
