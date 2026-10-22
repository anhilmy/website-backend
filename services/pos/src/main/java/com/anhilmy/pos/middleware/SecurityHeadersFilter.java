package com.anhilmy.pos.middleware;

import com.anhilmy.pos.config.ConfigManager;

import spark.Request;
import spark.Response;

/** Adds hardening response headers to every reply. */
public final class SecurityHeadersFilter {

    private SecurityHeadersFilter() {
    }

    public static void apply(Request req, Response res) {
        if (!ConfigManager.getBoolean("server.security-headers.enabled", true)) {
            return;
        }
        res.header("X-Content-Type-Options", "nosniff");
        res.header("X-Frame-Options", "DENY");
        res.header("Referrer-Policy", "no-referrer");
        res.header("X-XSS-Protection", "1; mode=block");

        // The interactive API docs intentionally load Swagger UI assets from a CDN, so a strict
        // default-src 'self' would render the page blank. Apply the strict policy everywhere else.
        String path = req.pathInfo();
        boolean isDocs = "/docs".equals(path) || "/docs/".equals(path)
                || "/openapi.yaml".equals(path);
        if (!isDocs) {
            res.header("Content-Security-Policy", "default-src 'self'");
        }
    }
}
