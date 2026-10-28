package com.anhilmy.pos.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyConverterTest {

    @Test
    void moneyRoundTripsThroughMinorUnits() {
        MoneyConverter converter = new MoneyConverter();

        assertEquals(1_234_567L, converter.convertToDatabaseColumn(
                new BigDecimal("12345.67")));
        assertEquals(0, new BigDecimal("12345.67").compareTo(
                converter.convertToEntityAttribute(1_234_567L)));
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void moneyRoundsToTwoDecimals() {
        MoneyConverter converter = new MoneyConverter();

        assertEquals(1L, converter.convertToDatabaseColumn(new BigDecimal("0.005")));
        assertEquals(0, new BigDecimal("0.01").compareTo(
                converter.convertToEntityAttribute(1L)));
    }

    @Test
    void rateRoundTripsPercentagesAndFixedValues() {
        RateConverter converter = new RateConverter();

        assertEquals(100_000L, converter.convertToDatabaseColumn(new BigDecimal("0.10")));
        assertEquals(0, new BigDecimal("0.10").compareTo(
                converter.convertToEntityAttribute(100_000L)));
        assertEquals(5_000_000_000L,
                converter.convertToDatabaseColumn(new BigDecimal("5000.00")));
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
