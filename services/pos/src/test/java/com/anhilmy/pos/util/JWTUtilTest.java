package com.anhilmy.pos.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.anhilmy.pos.exception.AuthenticationException;

import io.jsonwebtoken.Claims;

class JWTUtilTest {

    @Test
    void generatesAndParsesToken() {
        String token = JWTUtil.generateToken(7L, "cashier", "CASHIER");
        Claims claims = JWTUtil.validateAndParse(token);

        assertEquals("cashier", claims.getSubject());
        assertEquals("CASHIER", claims.get("role", String.class));
        assertEquals(7L, claims.get("uid", Long.class));
    }

    @Test
    void rejectsTamperedToken() {
        String token = JWTUtil.generateToken(1L, "manager", "MANAGER");
        // Flip a character in the middle of the signature segment. (Changing the trailing
        // base64 chars can decode to identical bytes, which made this test flaky.)
        int sigStart = token.lastIndexOf('.') + 1;
        int pos = sigStart + (token.length() - sigStart) / 2;
        char original = token.charAt(pos);
        char replacement = original == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, pos) + replacement + token.substring(pos + 1);
        assertNotEquals(token, tampered);
        assertThrows(AuthenticationException.class, () -> JWTUtil.validateAndParse(tampered));
    }

    @Test
    void rejectsMissingToken() {
        assertThrows(AuthenticationException.class, () -> JWTUtil.validateAndParse(null));
    }

    @Test
    void revokedTokenIsRejected() {
        String token = JWTUtil.generateToken(2L, "cashier2", "CASHIER");
        assertFalse(JWTUtil.isRevoked(token));
        JWTUtil.revoke(token);
        assertTrue(JWTUtil.isRevoked(token));
        assertThrows(AuthenticationException.class, () -> JWTUtil.validateAndParse(token));
    }
}
