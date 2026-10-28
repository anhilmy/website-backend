package com.anhilmy.pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores charge template values as scaled integers.
 *
 * <p>A charge value is either a percentage rate ({@code 0.10} = 10%) or a fixed amount. Six
 * decimal places cover both without any of SQLite's binary floating point drift: a stored
 * {@code 100000} reads back as exactly {@code 0.10}, and a fixed {@code 5000} reads back as
 * {@code 5000.00}.
 */
@Converter
public class RateConverter implements AttributeConverter<BigDecimal, Long> {

    /** Decimal places kept for charge values (percentages and fixed amounts). */
    public static final int SCALE = 6;

    private static final BigDecimal FACTOR = BigDecimal.TEN.pow(SCALE);

    @Override
    public Long convertToDatabaseColumn(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP)
                .multiply(FACTOR)
                .longValueExact();
    }

    @Override
    public BigDecimal convertToEntityAttribute(Long stored) {
        return stored == null ? null : BigDecimal.valueOf(stored, SCALE);
    }
}
