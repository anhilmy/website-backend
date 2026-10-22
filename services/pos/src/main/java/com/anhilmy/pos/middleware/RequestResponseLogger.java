package com.anhilmy.pos.middleware;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.event.Level;

import com.anhilmy.pos.config.ConfigManager;
import com.anhilmy.pos.logging.LoggingManager;

import spark.Request;
import spark.Response;

/**
 * Logs every request/response with timing and an end-to-end correlation id.
 *
 * <p>The correlation id is taken from the {@code X-Correlation-Id} request header when present,
 * otherwise generated, and echoed back on the response so clients and services can trace a
 * request across hops.
 */
public final class RequestResponseLogger {

    public static final String ATTR_CORRELATION_ID = "correlationId";
    public static final String ATTR_START = "requestStartNanos";

    private RequestResponseLogger() {
    }

    public static void before(Request req, Response res) {
        String correlationId = req.headers("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        req.attribute(ATTR_CORRELATION_ID, correlationId);
        req.attribute(ATTR_START, System.nanoTime());
        res.header("X-Correlation-Id", correlationId);

        if (ConfigManager.getBoolean("log.payload.enabled", false)) {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("correlationId", correlationId);
            fields.put("method", req.requestMethod());
            fields.put("path", req.pathInfo());
            fields.put("query", req.queryString());
            fields.put("remoteIp", req.ip());
            LoggingManager.event(Level.DEBUG, "http.request", fields);
        }
    }

    public static void after(Request req, Response res) {
        Object startAttr = req.attribute(ATTR_START);
        long durationMs = 0;
        if (startAttr instanceof Long start) {
            durationMs = (System.nanoTime() - start) / 1_000_000;
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("correlationId", req.attribute(ATTR_CORRELATION_ID));
        fields.put("method", req.requestMethod());
        fields.put("path", req.pathInfo());
        fields.put("status", res.status());
        fields.put("durationMs", durationMs);

        Level level = res.status() >= 500 ? Level.ERROR
                : res.status() >= 400 ? Level.WARN : Level.INFO;
        LoggingManager.event(level, "http.response", fields);
    }
}
