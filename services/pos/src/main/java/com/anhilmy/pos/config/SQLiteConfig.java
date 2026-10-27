package com.anhilmy.pos.config;

import java.util.Properties;

public class SQLiteConfig extends DatabaseConfig {
    @Override
    public Properties getHibernateProperties() {
        Properties props = new Properties();
        props.setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC");
        props.setProperty("hibernate.dialect", getDialect());
        props.setProperty("hibernate.connection.url", sqliteUrl(ConfigManager.get("db.url")));
        props.setProperty("hibernate.hbm2ddl.auto", ConfigManager.get("db.hbm2ddl", "update"));
        props.setProperty("hibernate.show_sql", ConfigManager.get("log.show_sql"));
        // SQLite serialises writers; a single connection plus a busy timeout avoids lock storms.
        props.setProperty("hibernate.hikari.maximumPoolSize", "1");
        props.setProperty("hibernate.hikari.idleTimeout", "30000");
        return props;
    }

    private static String sqliteUrl(String url) {
        if (url == null || !url.startsWith("jdbc:sqlite:") || url.contains("busy_timeout=")) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + "busy_timeout=5000&journal_mode=WAL";
    }

    @Override
    public String getDialect() {
        // SQLite dialect ships in hibernate-community-dialects
        return "org.hibernate.community.dialect.SQLiteDialect";
    }

    @Override
    public String getDriverClass() {
        return "org.sqlite.JDBC";
    }
}
