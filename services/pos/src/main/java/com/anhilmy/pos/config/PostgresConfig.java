package com.anhilmy.pos.config;

import java.util.Properties;

public class PostgresConfig extends DatabaseConfig {
    @Override
    public Properties getHibernateProperties() {
        Properties props = new Properties();
        props.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        props.setProperty("hibernate.dialect", getDialect());
        props.setProperty("hibernate.connection.url", ConfigManager.get("db.url"));
        props.setProperty("hibernate.connection.username", ConfigManager.get("db.username"));
        props.setProperty("hibernate.connection.password", ConfigManager.get("db.password"));
        props.setProperty("hibernate.hbm2ddl.auto", "validate");
        props.setProperty("hibernate.show_sql", ConfigManager.get("log.show_sql"));
        // Connection pooling (HikariCP)
        props.setProperty("hibernate.hikari.maximumPoolSize", "10");
        props.setProperty("hibernate.hikari.idleTimeout", "30000");
        return props;
    }

    @Override
    public String getDialect() {
        return "org.hibernate.dialect.PostgreSQLDialect";
    }

    @Override
    public String getDriverClass() {
        return "org.postgresql.Driver";
    }
}
