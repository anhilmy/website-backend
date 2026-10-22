package com.anhilmy.pos.db;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anhilmy.pos.config.ConfigManager;
import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.MenuCategoryRepository;
import com.anhilmy.pos.repository.MenuItemRepository;
import com.anhilmy.pos.repository.UserRepository;
import com.anhilmy.pos.util.PasswordUtil;

/**
 * Idempotent startup seeding.
 *
 * <p>Creates a default manager and cashier account plus a small sample menu when the database is
 * empty. Disabled by setting {@code seed.enabled=false} (and always skipped in {@code prod}).
 */
public final class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private DataSeeder() {
    }

    public static void seed() {
        if (!ConfigManager.getBoolean("seed.enabled", true)) {
            return;
        }
        if ("prod".equalsIgnoreCase(ConfigManager.activeProfile())) {
            log.info("Skipping data seed in prod profile");
            return;
        }

        DbManager.openSession();
        try {
            seedUsers();
            seedMenu();
            DbManager.commitAndClose();
        } catch (RuntimeException e) {
            DbManager.rollbackAndClose();
            log.error("Seeding failed", e);
            throw e;
        }
    }

    private static void seedUsers() {
        if (!ConfigManager.getBoolean("seed.demo-users", false)) {
            return;
        }
        UserRepository users = new UserRepository();
        if (users.count() > 0) {
            return;
        }
        users.save(user("manager", "manager123", Roles.MANAGER));
        users.save(user("cashier", "cashier123", Roles.CASHIER));
        log.warn("Seeded DEMO users (manager, cashier). Change these credentials immediately.");
    }

    private static void seedMenu() {
        MenuCategoryRepository categories = new MenuCategoryRepository();
        if (categories.count() > 0) {
            return;
        }
        MenuItemRepository items = new MenuItemRepository();

        MenuCategory beverages = categories.save(category("Beverages", "Hot and cold drinks"));
        MenuCategory mains = categories.save(category("Main Course", "Primary dishes"));
        MenuCategory desserts = categories.save(category("Desserts", "Sweet dishes"));

        items.save(item("Espresso", "Single shot espresso", "2.50", beverages));
        items.save(item("Grilled Chicken", "Chicken breast with vegetables", "12.00", mains));
        items.save(item("Chocolate Cake", "Slice of chocolate cake", "5.00", desserts));
        log.info("Seeded sample menu categories and items");
    }

    private static User user(String username, String rawPassword, String role) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(PasswordUtil.hash(rawPassword));
        u.setRole(role);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        return u;
    }

    private static MenuCategory category(String name, String description) {
        MenuCategory c = new MenuCategory();
        c.setName(name);
        c.setDescription(description);
        c.setActive(true);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return c;
    }

    private static MenuItem item(String name, String description, String price,
            MenuCategory category) {
        MenuItem i = new MenuItem();
        i.setName(name);
        i.setDescription(description);
        i.setPrice(new BigDecimal(price));
        i.setMenuCategory(category);
        i.setAvailable(true);
        i.setCreatedAt(LocalDateTime.now());
        i.setUpdatedAt(LocalDateTime.now());
        return i;
    }
}
