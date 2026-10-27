package com.anhilmy.pos.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import com.anhilmy.pos.config.ConfigManager;
import com.anhilmy.pos.config.DatabaseConfig;
import com.anhilmy.pos.config.PostgresConfig;
import com.anhilmy.pos.config.SQLiteConfig;
import com.anhilmy.pos.model.Charge;
import com.anhilmy.pos.model.ChargeTemplate;
import com.anhilmy.pos.model.DiningTable;
import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.PaymentMethod;
import com.anhilmy.pos.model.Refund;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.model.TransactionItem;
import com.anhilmy.pos.model.User;

@SuppressWarnings("ConvertToStringSwitch")
public class HibernateUtil {
    private static final SessionFactory sessionFactory;


    static {
        try {
            String dbType = ConfigManager.get("db.type");
            DatabaseConfig dbConfig;
            if (dbType == null) {
                throw new RuntimeException("db.type is not set in configuration");
            }
            dbType = dbType.toLowerCase();
            if (dbType.equals("sqlite")) {
                dbConfig = new SQLiteConfig();
            } else if (dbType.equals("postgresql") || dbType.equals("postgres")) {
                dbConfig = new PostgresConfig();
            } else {
                throw new RuntimeException("Unsupported db.type: " + dbType);
            }

            Configuration configuration = new Configuration();
            configuration.setProperties(dbConfig.getHibernateProperties());

            // Add annotated classes
            configuration.addAnnotatedClass(MenuItem.class);
            configuration.addAnnotatedClass(MenuCategory.class);
            configuration.addAnnotatedClass(Transaction.class);
            configuration.addAnnotatedClass(TransactionItem.class);
            configuration.addAnnotatedClass(User.class);
            configuration.addAnnotatedClass(DiningTable.class);
            configuration.addAnnotatedClass(PaymentMethod.class);
            configuration.addAnnotatedClass(ChargeTemplate.class);
            configuration.addAnnotatedClass(Charge.class);
            configuration.addAnnotatedClass(Refund.class);

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties()).build();

            sessionFactory = configuration.buildSessionFactory(serviceRegistry);

        } catch (RuntimeException ex) {
            System.err.println(
                    "[HibernateUtil] Error initializing SessionFactory: " + ex.getMessage());
            throw new ExceptionInInitializerError("Initial SessionFactory creation failed: " + ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
    // ...existing code...
}
