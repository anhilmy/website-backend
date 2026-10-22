package com.anhilmy.pos.util;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt password hashing and verification. */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
    }

    public static boolean matches(String rawPassword, String hash) {
        if (rawPassword == null || hash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, hash);
        } catch (IllegalArgumentException e) {
            // Malformed hash in the database - treat as non-matching rather than crashing.
            return false;
        }
    }
}
