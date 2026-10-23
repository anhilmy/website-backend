package com.anhilmy.pos.middleware;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.anhilmy.pos.exception.AuthenticationException;
import com.anhilmy.pos.exception.AuthorizationException;
import com.anhilmy.pos.util.JWTUtil;

import io.jsonwebtoken.Claims;
import spark.Request;
import spark.Response;

/**
 * Request authentication/authorization.
 *
 * <p>Extracts the bearer token, validates it and injects the user id, username and role as request
 * attributes. Endpoints listed in {@link #PUBLIC} are allowed without a token.
 */
public final class AuthenticationFilter {

    public static final String ATTR_USER_ID = "auth.userId";
    public static final String ATTR_USERNAME = "auth.username";
    public static final String ATTR_ROLE = "auth.role";

    /** "METHOD path" entries reachable without authentication. */
    private static final Set<String> PUBLIC = new HashSet<>(Arrays.asList(
            "GET /api/v1/healthcheck",
            "GET /api/v1/healthcheck/ready",
            "GET /health",
            "POST /api/v1/auth/login",
            "GET /api/v1/menu/categories",
            "GET /api/v1/menu/items",
            "GET /api/v1/menu/items/:id"));

    /**
     * Known API surface prefixes. Requests outside these are left to Spark's not-found handler
     * (404) instead of being rejected as unauthenticated.
     */
    private static final java.util.List<String> KNOWN_PREFIXES = Arrays.asList(
            "/api/v1/healthcheck",
            "/api/v1/auth",
            "/api/v1/menu",
            "/api/v1/categories",
            "/api/v1/items",
            "/api/v1/orders",
            "/api/v1/tables",
            "/api/v1/users");

    private AuthenticationFilter() {
    }

    public static boolean isPublic(String method, String path) {
        if ("OPTIONS".equals(method)) {
            // CORS preflight must succeed without credentials.
            return true;
        }
        if (PUBLIC.contains(method + " " + path)) {
            return true;
        }
        // Public item lookup with an id: GET /api/v1/menu/items/{id}
        return "GET".equals(method) && path.matches("/api/v1/menu/items/\\d+");
    }

    private static boolean isKnownApiPath(String path) {
        for (String prefix : KNOWN_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    /** Spark before-filter body. */
    public static void requireAuth(Request req, Response res) {
        if (isPublic(req.requestMethod(), req.pathInfo())) {
            return;
        }
        // Unknown paths fall through to the 404 handler rather than a misleading 401.
        if (!isKnownApiPath(req.pathInfo())) {
            return;
        }
        String header = req.headers("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AuthenticationException("Missing or malformed Authorization header");
        }
        String token = header.substring("Bearer ".length()).trim();
        Claims claims = JWTUtil.validateAndParse(token);

        req.attribute(ATTR_USER_ID, claims.get("uid", Long.class));
        req.attribute(ATTR_USERNAME, claims.getSubject());
        req.attribute(ATTR_ROLE, claims.get("role", String.class));
        req.attribute("auth.token", token);
    }

    public static Long userId(Request req) {
        return req.attribute(ATTR_USER_ID);
    }

    public static String username(Request req) {
        return req.attribute(ATTR_USERNAME);
    }

    public static String role(Request req) {
        return req.attribute(ATTR_ROLE);
    }

    public static String token(Request req) {
        return req.attribute("auth.token");
    }

    public static void requireRole(Request req, String... roles) {
        String actual = role(req);
        for (String r : roles) {
            if (r.equals(actual)) {
                return;
            }
        }
        throw new AuthorizationException("Requires one of roles: " + String.join(",", roles));
    }
}
