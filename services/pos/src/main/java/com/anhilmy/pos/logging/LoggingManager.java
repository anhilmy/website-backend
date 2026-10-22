package com.anhilmy.pos.logging;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.anhilmy.pos.util.Json;

/**
 * Central entry point for application logging.
 *
 * <p>Human-readable lines go to SLF4J (console + rotating file via {@code logback.xml}) while
 * structured JSON events are additionally streamed to Kafka when enabled. Business/audit events
 * use {@link #audit(String, Map)} to keep a consistent, greppable shape.
 */
public final class LoggingManager {

    private static final Logger log = LoggerFactory.getLogger(LoggingManager.class);

    private LoggingManager() {
    }

    public static Logger logger(Class<?> type) {
        return LoggerFactory.getLogger(type);
    }

    /** Emits a structured event to the log and (optionally) Kafka. */
    public static void event(Level level, String event, Map<String, Object> fields) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("event", event);
        if (fields != null) {
            payload.putAll(fields);
        }
        String json = Json.gson().toJson(payload);

        switch (level) {
            case ERROR -> log.error(json);
            case WARN -> log.warn(json);
            case DEBUG -> log.debug(json);
            case TRACE -> log.trace(json);
            default -> log.info(json);
        }

        KafkaLogger.send(event, json);
    }

    /** Records a business/audit action for sensitive operations. */
    public static void audit(String action, Map<String, Object> fields) {
        Map<String, Object> payload = fields == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fields);
        payload.put("audit", true);
        event(Level.INFO, action, payload);
    }

    public static void shutdown() {
        KafkaLogger.close();
    }
}
