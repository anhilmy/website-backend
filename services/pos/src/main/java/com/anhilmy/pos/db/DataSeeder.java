package com.anhilmy.pos.db;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anhilmy.pos.config.ConfigManager;
import com.anhilmy.pos.model.ChargeBasis;
import com.anhilmy.pos.model.ChargeCalcType;
import com.anhilmy.pos.model.ChargeTemplate;
import com.anhilmy.pos.model.DiningTable;
import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.PaymentMethod;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.model.TableStatus;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.ChargeTemplateRepository;
import com.anhilmy.pos.repository.DiningTableRepository;
import com.anhilmy.pos.repository.MenuCategoryRepository;
import com.anhilmy.pos.repository.MenuItemRepository;
import com.anhilmy.pos.repository.PaymentMethodRepository;
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
            seedPaymentMethods();
            seedChargeTemplates();
            seedMenu();
            seedTables();
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

        items.save(item("Espresso", "Single shot espresso", "18000", beverages));
        items.save(item("Grilled Chicken", "Chicken breast with vegetables", "45000", mains));
        items.save(item("Chocolate Cake", "Slice of chocolate cake", "30000", desserts));
        log.info("Seeded sample menu categories and items");
    }

    private static void seedPaymentMethods() {
        PaymentMethodRepository methods = new PaymentMethodRepository();
        if (methods.count() > 0) {
            return;
        }
        methods.save(paymentMethod("CASH", "Cash", true, false, 10));
        methods.save(paymentMethod("CARD", "Card", false, false, 20));
        methods.save(paymentMethod("QRIS", "QRIS", false, false, 30));
        methods.save(paymentMethod("EWALLET", "E-Wallet", false, false, 40));
        methods.save(paymentMethod("TRANSFER", "Bank Transfer", false, true, 50));
        log.info("Seeded default payment methods");
    }

    private static void seedChargeTemplates() {
        ChargeTemplateRepository templates = new ChargeTemplateRepository();
        if (templates.count() > 0) {
            return;
        }
        ChargeTemplate tax = new ChargeTemplate();
        tax.setCode("TAX");
        tax.setLabel("Tax");
        tax.setCalcType(ChargeCalcType.PERCENTAGE);
        tax.setValue(new BigDecimal(ConfigManager.get("order.tax-rate", "0.10")));
        tax.setBasis(ChargeBasis.SUBTOTAL);
        tax.setSortOrder(10);
        tax.setTaxable(false);
        tax.setIsTax(true);
        tax.setActive(true);
        tax.setCreatedAt(LocalDateTime.now());
        tax.setUpdatedAt(LocalDateTime.now());
        templates.save(tax);
        log.info("Seeded default charge template (Tax)");
    }

    private static void seedTables() {
        DiningTableRepository tables = new DiningTableRepository();
        if (tables.count() > 0) {
            return;
        }
        String[][] layout = {
                {"T1", "Indoor"}, {"T2", "Indoor"}, {"T3", "Indoor"}, {"T4", "Indoor"},
                {"P1", "Patio"}, {"P2", "Patio"},
                {"B1", "Bar"}, {"B2", "Bar"},
        };
        for (String[] t : layout) {
            DiningTable table = new DiningTable();
            table.setName(t[0]);
            table.setArea(t[1]);
            table.setStatus(TableStatus.AVAILABLE);
            table.setCreatedAt(LocalDateTime.now());
            table.setUpdatedAt(LocalDateTime.now());
            tables.save(table);
        }
        log.info("Seeded {} dining tables", layout.length);
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

    private static PaymentMethod paymentMethod(String code, String label, boolean isCash,
            boolean requiresReference, int sortOrder) {
        PaymentMethod m = new PaymentMethod();
        m.setCode(code);
        m.setLabel(label);
        m.setActive(true);
        m.setIsCash(isCash);
        m.setRequiresReference(requiresReference);
        m.setSortOrder(sortOrder);
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        return m;
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
