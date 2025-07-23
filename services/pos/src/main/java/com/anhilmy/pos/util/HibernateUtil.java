package com.anhilmy.pos.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.model.TransactionItem;
import com.anhilmy.pos.model.User;

import io.github.cdimascio.dotenv.Dotenv;

public class HibernateUtil {
    private static final SessionFactory sessionFactory;

    static {
        try {
            // Load .env file
            Dotenv dotenv = Dotenv.load();

            Configuration configuration = new Configuration();

            // Set properties from .env
            configuration.setProperty("hibernate.connection.driver_class", dotenv.get("DB_DRIVER"));
            configuration.setProperty("hibernate.connection.url", dotenv.get("DB_URL"));
            configuration.setProperty("hibernate.connection.username", dotenv.get("DB_USERNAME"));
            configuration.setProperty("hibernate.connection.password", dotenv.get("DB_PASSWORD"));
            configuration.setProperty("hibernate.dialect", dotenv.get("DB_DIALECT"));
            configuration.setProperty("hibernate.hbm2ddl.auto", "update");
            configuration.setProperty("show_sql",
                    "true".equals(dotenv.get("DEVELOPMENT")) ? "true" : "false");

            // Add annotated classes
            configuration.addAnnotatedClass(MenuItem.class);
            configuration.addAnnotatedClass(MenuCategory.class);
            configuration.addAnnotatedClass(Transaction.class);
            configuration.addAnnotatedClass(TransactionItem.class);
            configuration.addAnnotatedClass(User.class);

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties()).build();

            sessionFactory = configuration.buildSessionFactory(serviceRegistry);

        } catch (Throwable ex) {
            throw new ExceptionInInitializerError("Initial SessionFactory creation failed: " + ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
