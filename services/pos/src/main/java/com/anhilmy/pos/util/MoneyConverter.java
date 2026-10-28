package com.anhilmy.pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores monetary values as integer minor units (sen) in both databases.
 *
 * <p>SQLite has no exact decimal type and its {@code REAL} storage is a binary floating point
 * value, while PostgreSQL uses {@code NUMERIC}. Storing {@code BigDecimal} as a scaled
 * {@code BIGINT} gives both dialects the same exact representation and keeps every amount within
 * one well-defined rounding step.
 */
@Converter
public class MoneyConverter implements AttributeConverter<BigDecimal, Long> {

    /** Number of decimal places kept for money (1.00 = 100 minor units). */
    public static final int SCALE = 2;

    private static final BigDecimal FACTOR = BigDecimal.TEN.pow(SCALE);

    @Override
    public Long convertToDatabaseColumn(BigDecimal value) {
        return toMinor(value);
    }

    @Override
    public BigDecimal convertToEntityAttribute(Long stored) {
        return fromMinor(stored);
    }

    /** Converts a decimal amount to integer minor units. Null stays null. */
    public static Long toMinor(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP)
                .multiply(FACTOR)
                .longValueExact();
    }

    /** Converts integer minor units back to a two-decimal amount. Null stays null. */
    public static BigDecimal fromMinor(Long stored) {
        return stored == null ? null : BigDecimal.valueOf(stored, SCALE);
    }
}
