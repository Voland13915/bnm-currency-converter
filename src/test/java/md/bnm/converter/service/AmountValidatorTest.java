package md.bnm.converter.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AmountValidatorTest {

    @Test
    @DisplayName("T031: Valid amount with period separator")
    void testValidPeriodSeparator() {
        AmountValidator.ValidationResult result = AmountValidator.validate("100.50");
        assertTrue(result.isValid());
        assertNull(result.errorMessage());
        assertEquals(new BigDecimal("100.50"), result.amount());
    }

    @Test
    @DisplayName("T031: Valid amount with comma separator")
    void testValidCommaSeparator() {
        AmountValidator.ValidationResult result = AmountValidator.validate("100,50");
        assertTrue(result.isValid());
        assertNull(result.errorMessage());
        assertEquals(new BigDecimal("100.50"), result.amount());
    }

    @Test
    @DisplayName("T031: Valid integer amount without decimals")
    void testValidIntegerAmount() {
        AmountValidator.ValidationResult result = AmountValidator.validate("500");
        assertTrue(result.isValid());
        assertEquals(new BigDecimal("500"), result.amount());
    }

    @Test
    @DisplayName("T031: Valid amount with one decimal digit")
    void testValidOneDecimal() {
        AmountValidator.ValidationResult resultDot = AmountValidator.validate("10.5");
        assertTrue(resultDot.isValid());
        assertEquals(new BigDecimal("10.5"), resultDot.amount());

        AmountValidator.ValidationResult resultComma = AmountValidator.validate("10,5");
        assertTrue(resultComma.isValid());
        assertEquals(new BigDecimal("10.5"), resultComma.amount());
    }

    @Test
    @DisplayName("T031: Valid amount with whitespace padding")
    void testValidWhitespace() {
        AmountValidator.ValidationResult result = AmountValidator.validate("   250.75   ");
        assertTrue(result.isValid());
        assertEquals(new BigDecimal("250.75"), result.amount());
    }

    @Test
    @DisplayName("T031: Minimum positive amount and maximum allowed amount 1,000,000,000")
    void testBoundaryAmounts() {
        AmountValidator.ValidationResult minResult = AmountValidator.validate("0.01");
        assertTrue(minResult.isValid());
        assertEquals(new BigDecimal("0.01"), minResult.amount());

        AmountValidator.ValidationResult maxResult = AmountValidator.validate("1000000000");
        assertTrue(maxResult.isValid());
        assertEquals(new BigDecimal("1000000000"), maxResult.amount());

        AmountValidator.ValidationResult maxResultDecimals = AmountValidator.validate("1000000000.00");
        assertTrue(maxResultDecimals.isValid());
        assertEquals(new BigDecimal("1000000000.00"), maxResultDecimals.amount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("T031: Empty or blank amounts rejected")
    void testEmptyAmounts(String input) {
        AmountValidator.ValidationResult result = AmountValidator.validate(input);
        assertFalse(result.isValid());
        assertNotNull(result.errorMessage());
        assertEquals("Введите сумму для конвертации.", result.errorMessage());
    }

    @Test
    @DisplayName("T031: Null amount rejected")
    void testNullAmount() {
        AmountValidator.ValidationResult result = AmountValidator.validate(null);
        assertFalse(result.isValid());
        assertNotNull(result.errorMessage());
        assertEquals("Введите сумму для конвертации.", result.errorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12a", "$100", "100 MDL", "one"})
    @DisplayName("T031: Non-numeric strings containing letters/symbols rejected")
    void testNonNumeric(String input) {
        AmountValidator.ValidationResult result = AmountValidator.validate(input);
        assertFalse(result.isValid());
        assertEquals("Сумма должна быть числом.", result.errorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "0,00", "-0", "-0.00"})
    @DisplayName("T031: Zero amounts rejected")
    void testZeroAmounts(String input) {
        AmountValidator.ValidationResult result = AmountValidator.validate(input);
        assertFalse(result.isValid());
        assertEquals("Сумма должна быть больше нуля.", result.errorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-0.01", "-100,50", "-50.25"})
    @DisplayName("T031: Negative amounts rejected")
    void testNegativeAmounts(String input) {
        AmountValidator.ValidationResult result = AmountValidator.validate(input);
        assertFalse(result.isValid());
        assertEquals("Сумма должна быть больше нуля.", result.errorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.123", "5,999", "0.001", "100.12345"})
    @DisplayName("T031: More than two decimal places rejected")
    void testOverPrecision(String input) {
        AmountValidator.ValidationResult result = AmountValidator.validate(input);
        assertFalse(result.isValid());
        assertEquals("Допускается не более двух знаков после запятой.", result.errorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.5,2", "1,000.50", "1.000,50"})
    @DisplayName("T031: Mixed period and comma separators rejected")
    void testMixedSeparators(String input) {
        AmountValidator.ValidationResult result = AmountValidator.validate(input);
        assertFalse(result.isValid());
        assertTrue(result.errorMessage().contains("Некорректный формат числа"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.5.2", "10,5,2", "..5", ",,5"})
    @DisplayName("T031: Multiple decimal separators rejected")
    void testMultipleSeparators(String input) {
        AmountValidator.ValidationResult result = AmountValidator.validate(input);
        assertFalse(result.isValid());
        assertEquals("Некорректный формат числа.", result.errorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1000000000.01", "1000000001", "2000000000", "999999999999"})
    @DisplayName("T031: Amounts strictly above 1,000,000,000 rejected")
    void testAboveMaximum(String input) {
        AmountValidator.ValidationResult result = AmountValidator.validate(input);
        assertFalse(result.isValid());
        assertEquals("Сумма не должна превышать 1 000 000 000.", result.errorMessage());
    }
}
