package com.anhilmy.pos.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {

    @Test
    void hashVerifiesAgainstOriginal() {
        String hash = PasswordUtil.hash("s3cretPass");
        assertNotEquals("s3cretPass", hash);
        assertTrue(PasswordUtil.matches("s3cretPass", hash));
        assertFalse(PasswordUtil.matches("wrongPass", hash));
    }

    @Test
    void malformedHashDoesNotThrow() {
        assertFalse(PasswordUtil.matches("anything", "not-a-bcrypt-hash"));
    }
}
