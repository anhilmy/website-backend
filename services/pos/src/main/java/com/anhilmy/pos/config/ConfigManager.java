package com.anhilmy.pos.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Environment-aware configuration access.
 *
 * <p>Resolution order for a key (e.g. {@code db.url}):
 * <ol>
 * <li>Environment variable derived from the key ({@code db.url} -> {@code DB_URL}).</li>
 * <li>Optional profile overlay file: {@code application-<PROFILE>.properties}.</li>
 * <li>Base {@code application.properties} on the classpath.</li>
 * </ol>
 *
 * <p>The active profile is read from the {@code PROFILE} environment variable
 * (default {@code dev}).
 */
public final class ConfigManager {

    private static final String BASE_CONFIG_FILE = "/application.properties";
    private static final Properties properties = new Properties();

    static {
        String profile = profile();
        // Base config first, then profile overlay so the profile wins.
        loadInto(properties, BASE_CONFIG_FILE, true);
        loadInto(properties, "/application-" + profile + ".properties", false);
    }

    private ConfigManager() {
    }

    private static String profile() {
        String p = System.getenv("PROFILE");
        if (p == null || p.isBlank()) {
            p = System.getProperty("profile", "dev");
        }
        return p.trim();
    }

    private static void loadInto(Properties target, String resource, boolean required) {
        try (InputStream input = ConfigManager.class.getResourceAsStream(resource)) {
            if (input != null) {
                target.load(input);
            } else if (required) {
                throw new IllegalStateException("Configuration file not found: " + resource);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load configuration: " + resource, e);
        }
    }

    public static String get(String key) {
        String raw = getRaw(key);
        return resolvePlaceholders(raw);
    }

    /** Returns the value without placeholder interpolation (internal use). */
    private static String getRaw(String key) {
        String envValue = System.getenv(key.replace('.', '_').toUpperCase());
        if (envValue != null) {
            return envValue;
        }
        return properties.getProperty(key);
    }

    /**
     * Resolves {@code ${key}} placeholders and {@code ${env:VAR}} references inside a value.
     */
    private static String resolvePlaceholders(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        String result = value;
        int guard = 0;
        int start;
        while ((start = result.indexOf("${")) >= 0 && guard++ < 10) {
            int end = result.indexOf('}', start);
            if (end < 0) {
                break;
            }
            String token = result.substring(start + 2, end);
            String replacement;
            if (token.startsWith("env:")) {
                replacement = System.getenv(token.substring(4));
            } else {
                replacement = getRaw(token);
            }
            if (replacement == null) {
                replacement = "";
            }
            result = result.substring(0, start) + replacement + result.substring(end + 1);
        }
        return result;
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : value;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long getLong(String key, long defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Fails fast when required keys are missing or insecure.
     *
     * <p>Refuses to start with the shipped placeholder JWT secret, since that would let anyone
     * mint valid tokens for a deployed instance.
     */
    public static void validate() {
        List<String> required = Arrays.asList("db.type", "db.url", "server.port", "jwt.secret");
        StringBuilder missing = new StringBuilder();
        for (String key : required) {
            String value = get(key);
            if (value == null || value.isBlank()) {
                missing.append(key).append(' ');
            }
        }
        if (missing.length() > 0) {
            throw new IllegalStateException("Missing required configuration keys: " + missing);
        }

        String secret = get("jwt.secret");
        String lowered = secret.toLowerCase();
        if (lowered.contains("change_me") || secret.contains("@@REQUIRED@@")
                || secret.length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret is insecure: set a unique random value of at least 32 characters "
                            + "(env JWT_SECRET). The placeholder default is not allowed.");
        }
    }

    public static String activeProfile() {
        return profile();
    }
}
