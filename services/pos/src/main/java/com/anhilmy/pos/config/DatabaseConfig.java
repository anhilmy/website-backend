package com.anhilmy.pos.config;

import java.util.Properties;

public abstract class DatabaseConfig {
    public abstract Properties getHibernateProperties();

    public abstract String getDialect();

    public abstract String getDriverClass();
}
