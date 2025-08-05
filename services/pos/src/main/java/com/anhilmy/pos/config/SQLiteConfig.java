package com.anhilmy.pos.config;

import java.util.Properties;

public class SQLiteConfig extends DatabaseConfig {
    @Override
    public Properties getHibernateProperties() {
        Properties props = new Properties();
        props.setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC");
        props.setProperty("hibernate.dialect", getDialect());
        props.setProperty("hibernate.connection.url", ConfigManager.get("db.url"));
        props.setProperty("hibernate.hbm2ddl.auto", "validate");
        props.setProperty("hibernate.show_sql", ConfigManager.get("log.show_sql"));
        // Connection pooling (HikariCP)
        props.setProperty("hibernate.hikari.maximumPoolSize", "5");
        props.setProperty("hibernate.hikari.idleTimeout", "30000");
        return props;
    }

    @Override
    public String getDialect() {
        return "org.hibernate.dialect.SQLiteDialect";
    }

    @Override
    public String getDriverClass() {
        return "org.sqlite.JDBC";
    }
}
