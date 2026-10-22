package com.anhilmy.pos.middleware;

import com.anhilmy.pos.config.ConfigManager;

import spark.Request;
import spark.Response;

/** Applies CORS headers from configuration and answers preflight requests. */
public final class CorsFilter {

    private CorsFilter() {
    }

    public static void apply(Request req, Response res) {
        if (!ConfigManager.getBoolean("server.cors.enabled", true)) {
            return;
        }
        String allowed = ConfigManager.get("server.cors.allowed-origins", "*");
        String origin = req.headers("Origin");

        if ("*".equals(allowed)) {
            res.header("Access-Control-Allow-Origin", "*");
        } else if (origin != null
                && ("," + allowed.replace(" ", "") + ",").contains("," + origin + ",")) {
            res.header("Access-Control-Allow-Origin", origin);
            res.header("Vary", "Origin");
        }

        res.header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        res.header("Access-Control-Allow-Headers",
                "Content-Type,Authorization,X-Correlation-Id");
        res.header("Access-Control-Max-Age", "3600");
    }

    /**
     * Preflight handler. CORS headers are already applied by the global {@code before} filter;
     * re-applying here would emit duplicate {@code Access-Control-Allow-Origin} values, which
     * browsers reject. So this just acknowledges the preflight.
     */
    public static Object preflight(Request req, Response res) {
        res.status(200);
        return "";
    }
}
