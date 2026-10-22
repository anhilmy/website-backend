package com.anhilmy.pos.util;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Shared, pre-configured {@link Gson} instance.
 *
 * <ul>
 * <li>ISO-8601 adapters for {@link LocalDateTime}/{@link LocalDate} (no reflective leakage).</li>
 * <li>Skips sensitive fields (password hashes) and bidirectional back-references that would
 * otherwise cause infinite recursion during serialization.</li>
 * </ul>
 */
public final class Json {

    /** Field names excluded from serialization (secrets and cyclic back-references). */
    private static final Set<String> SKIPPED_FIELDS = new HashSet<>(Arrays.asList(
            "passwordHash", // never expose credentials
            "menuItems", // MenuCategory -> items (cycle via MenuItem.menuCategory)
            "children", // MenuCategory self-hierarchy cycle
            "transaction" // TransactionItem -> Transaction back-reference (cycle)
    ));

    private static final ExclusionStrategy EXCLUSIONS = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return SKIPPED_FIELDS.contains(f.getName());
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    };

    private static final Gson GSON = new GsonBuilder()
            .addSerializationExclusionStrategy(EXCLUSIONS)
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .disableHtmlEscaping()
            .create();

    private Json() {
    }

    public static Gson gson() {
        return GSON;
    }

    private static class LocalDateTimeAdapter
            implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type type, JsonSerializationContext ctx) {
            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type type,
                JsonDeserializationContext ctx) {
            return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    private static class LocalDateAdapter
            implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        @Override
        public JsonElement serialize(LocalDate src, Type type, JsonSerializationContext ctx) {
            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type type,
                JsonDeserializationContext ctx) {
            return LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }
}
