package com.anhilmy.pos.logging;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anhilmy.pos.config.ConfigManager;

/**
 * Asynchronous log shipping to Kafka.
 *
 * <p>Disabled by default ({@code kafka.enabled=false}). When enabled, messages are produced
 * asynchronously and any failure is swallowed with a warning so logging can never take down the
 * request path.
 */
public final class KafkaLogger {

    private static final Logger log = LoggerFactory.getLogger(KafkaLogger.class);

    private static final AtomicBoolean INIT_ATTEMPTED = new AtomicBoolean(false);
    private static KafkaProducer<String, String> producer;
    private static String topic;

    private KafkaLogger() {
    }

    private static synchronized void init() {
        if (producer != null || !ConfigManager.getBoolean("kafka.enabled", false)) {
            return;
        }
        try {
            Properties props = new Properties();
            props.put("bootstrap.servers", ConfigManager.get("kafka.bootstrap.servers"));
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer",
                    "org.apache.kafka.common.serialization.StringSerializer");
            props.put("acks", "1");
            props.put("retries", "3");
            props.put("linger.ms", "50");
            props.put("max.block.ms", "2000");
            producer = new KafkaProducer<>(props);
            topic = ConfigManager.get("kafka.topic.logs", "pos-logs");
            log.info("Kafka logging enabled, topic={}", topic);
        } catch (RuntimeException e) {
            log.warn("Kafka logging could not be initialised: {}", e.getMessage());
            producer = null;
        }
    }

    public static void send(String key, String jsonPayload) {
        if (!ConfigManager.getBoolean("kafka.enabled", false)) {
            return;
        }
        if (!INIT_ATTEMPTED.getAndSet(true)) {
            init();
        }
        if (producer == null) {
            return;
        }
        try {
            producer.send(new ProducerRecord<>(topic, key, jsonPayload),
                    (metadata, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish log to Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Failed to publish log to Kafka: {}", e.getMessage());
        }
    }

    public static boolean isHealthy() {
        return !ConfigManager.getBoolean("kafka.enabled", false) || producer != null;
    }

    public static void close() {
        if (producer != null) {
            try {
                producer.flush();
                producer.close();
            } catch (RuntimeException e) {
                log.warn("Error closing Kafka producer: {}", e.getMessage());
            }
        }
    }
}
