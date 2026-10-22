package com.anhilmy.pos.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import com.anhilmy.pos.config.ConfigManager;
import com.anhilmy.pos.exception.AuthenticationException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Stateless JWT issuing and verification (HS256).
 *
 * <p>Tokens carry the user id, username and role. Logout is supported through an in-memory
 * blacklist of revoked tokens (keyed by the raw token, pruned on access once expired).
 */
public final class JWTUtil {

    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            ConfigManager.get("jwt.secret").getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_SECONDS = ConfigManager.getLong("jwt.expiration", 3600);
    private static final String ISSUER = ConfigManager.get("jwt.issuer", "pos-system");

    private static final Map<String, Long> REVOKED = new ConcurrentHashMap<>();

    private JWTUtil() {
    }

    public static String generateToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuer(ISSUER)
                .claim("uid", userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(EXPIRATION_SECONDS)))
                .signWith(KEY)
                .compact();
    }

    public static Claims validateAndParse(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("Missing authentication token");
        }
        if (isRevoked(token)) {
            throw new AuthenticationException("Token has been revoked");
        }
        try {
            return Jwts.parser()
                    .verifyWith(KEY)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthenticationException("Invalid or expired token");
        }
    }

    public static void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Claims claims = Jwts.parser().verifyWith(KEY).build()
                    .parseSignedClaims(token).getPayload();
            long expiresAt = claims.getExpiration().getTime();
            REVOKED.put(token, expiresAt);
            pruneExpired();
        } catch (JwtException | IllegalArgumentException e) {
            // Already invalid - nothing to revoke.
        }
    }

    public static boolean isRevoked(String token) {
        Long expiresAt = REVOKED.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt < System.currentTimeMillis()) {
            REVOKED.remove(token);
            return false;
        }
        return true;
    }

    private static void pruneExpired() {
        long now = System.currentTimeMillis();
        REVOKED.entrySet().removeIf(e -> e.getValue() < now);
    }

    public static long getExpirationSeconds() {
        return EXPIRATION_SECONDS;
    }
}
