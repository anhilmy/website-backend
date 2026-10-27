package com.anhilmy.pos.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import com.anhilmy.pos.exception.ValidationException;

/**
 * Small hand-rolled validation helpers used at controller/service boundaries.
 *
 * <p>Keeps validation logic in one place and throws {@link ValidationException} so the global
 * handler can map failures to HTTP 400 with a consistent error body.
 */
public final class InputValidator {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");
    /** At least 8 chars, one letter and one digit. */
    private static final Pattern PASSWORD_STRENGTH = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private InputValidator() {
    }

    public static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        return value.trim();
    }

    /** Non-blank and no longer than {@code max} characters. */
    public static String requireMaxLength(String value, String field, int max) {
        String clean = requireNonBlank(value, field);
        if (clean.length() > max) {
            throw new ValidationException(field + " must be at most " + max + " characters");
        }
        return clean;
    }

    /** Optional free text: sanitised and length-checked when present. */
    public static String sanitizeMax(String value, String field, int max) {
        String clean = sanitize(value);
        if (clean != null && clean.length() > max) {
            throw new ValidationException(field + " must be at most " + max + " characters");
        }
        return clean;
    }

    public static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new ValidationException(field + " is required");
        }
        return value;
    }

    public static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new ValidationException(field + " must be greater than zero");
        }
        return value;
    }

    public static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new ValidationException(field + " must be greater than zero");
        }
        return value;
    }

    public static int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new ValidationException(field + " must not be negative");
        }
        return value;
    }

    public static String requireUsername(String username) {
        String clean = requireNonBlank(username, "username");
        if (!USERNAME.matcher(clean).matches()) {
            throw new ValidationException(
                    "username must be 3-50 characters (letters, digits, . _ -)");
        }
        return clean;
    }

    public static String requireStrongPassword(String password) {
        requireNonBlank(password, "password");
        if (!PASSWORD_STRENGTH.matcher(password).matches()) {
            throw new ValidationException(
                    "password must be at least 8 characters and contain a letter and a digit");
        }
        return password;
    }

    /**
     * Trims and strips ASCII control characters (except tab/newline) from free-text input.
     */
    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\t' || c == '\n' || c == '\r' || c >= 0x20) {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    public static int parseId(String raw, String field) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a valid id");
        }
    }

    public static long parseLongId(String raw, String field) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a valid id");
        }
    }
}
