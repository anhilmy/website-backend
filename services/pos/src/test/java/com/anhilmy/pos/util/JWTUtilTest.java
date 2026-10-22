package com.anhilmy.pos.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        String tampered = token.substring(0, token.length() - 2) + "xx";
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
