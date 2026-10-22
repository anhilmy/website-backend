package com.anhilmy.pos.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.anhilmy.pos.exception.ValidationException;

class InputValidatorTest {

    @Test
    void rejectsBlankStrings() {
        assertThrows(ValidationException.class, () -> InputValidator.requireNonBlank("  ", "name"));
    }

    @Test
    void rejectsNonPositivePrice() {
        assertThrows(ValidationException.class,
                () -> InputValidator.requirePositive(BigDecimal.ZERO, "price"));
        assertThrows(ValidationException.class,
                () -> InputValidator.requirePositive(new BigDecimal("-1"), "price"));
    }

    @Test
    void validatesUsernameFormat() {
        assertEquals("cashier_1", InputValidator.requireUsername("cashier_1"));
        assertThrows(ValidationException.class, () -> InputValidator.requireUsername("ab"));
        assertThrows(ValidationException.class, () -> InputValidator.requireUsername("bad name!"));
    }

    @Test
    void validatesPasswordStrength() {
        assertEquals("abc12345", InputValidator.requireStrongPassword("abc12345"));
        assertThrows(ValidationException.class,
                () -> InputValidator.requireStrongPassword("short1"));
        assertThrows(ValidationException.class,
                () -> InputValidator.requireStrongPassword("nodigitshere"));
    }

    @Test
    void parsesIds() {
        assertEquals(42, InputValidator.parseId("42", "id"));
        assertThrows(ValidationException.class, () -> InputValidator.parseId("nope", "id"));
    }
}
