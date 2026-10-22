package com.anhilmy.pos.middleware;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.anhilmy.pos.config.ConfigManager;
import com.anhilmy.pos.exception.ApiException;

import spark.Request;

/**
 * In-memory fixed-window rate limiter keyed by client IP.
 *
 * <p>Suitable for a single-instance deployment; for horizontally scaled deployments a shared
 * store (e.g. Redis) should back this instead.
 */
public final class RateLimitFilter {

    private static final long WINDOW_MS = 60_000L;

    private final String name;
    private final int limitPerMinute;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    private RateLimitFilter(String name, int limitPerMinute) {
        this.name = name;
        this.limitPerMinute = limitPerMinute;
    }

    private static final RateLimitFilter GENERAL = new RateLimitFilter("general",
            ConfigManager.getInt("server.rate-limit.requests-per-minute", 120));

    private static final RateLimitFilter LOGIN = new RateLimitFilter("login",
            ConfigManager.getInt("server.rate-limit.login-attempts-per-minute", 10));

    public static void checkGeneral(Request req) {
        GENERAL.check(req);
    }

    public static void checkLogin(Request req) {
        LOGIN.check(req);
    }

    private void check(Request req) {
        if (limitPerMinute <= 0) {
            return;
        }
        String key = name + ":" + clientIp(req);
        long now = System.currentTimeMillis();
        Deque<Long> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > WINDOW_MS) {
                window.pollFirst();
            }
            if (window.size() >= limitPerMinute) {
                throw new ApiException(429, "RATE_LIMITED",
                        "Too many requests; try again later");
            }
            window.addLast(now);
        }
    }

    private static String clientIp(Request req) {
        String forwarded = req.headers("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.ip();
    }
}
