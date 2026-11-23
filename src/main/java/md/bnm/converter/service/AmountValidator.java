package md.bnm.converter.service;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Service for validating amount input according to data-model.md specifications.
 * Accepts period or comma decimal separator (not both), allows at most two fractional digits,
 * and enforces 0 < amount <= 1,000,000,000.
 */
public class AmountValidator {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");

    public record ValidationResult(BigDecimal amount, String errorMessage) {
        public boolean isValid() {
            return errorMessage == null;
        }

        public static ValidationResult success(BigDecimal amount) {
            return new ValidationResult(amount, null);
        }

        public static ValidationResult error(String errorMessage) {
            return new ValidationResult(null, errorMessage);
        }
    }

    public static ValidationResult validate(String input) {
        if (input == null || input.trim().isEmpty()) {
            return ValidationResult.error("Введите сумму для конвертации.");
        }

        String trimmed = input.trim();

        // Check for mixed separators
        boolean hasDot = trimmed.contains(".");
        boolean hasComma = trimmed.contains(",");
        if (hasDot && hasComma) {
            return ValidationResult.error("Некорректный формат числа (используйте точку или запятую, но не обе).");
        }

        // Check for multiple separators of the same kind
        int dotCount = countOccurrences(trimmed, '.');
        int commaCount = countOccurrences(trimmed, ',');
        if (dotCount > 1 || commaCount > 1) {
            return ValidationResult.error("Некорректный формат числа.");
        }

        // Check decimal places
        int separatorIdx = hasDot ? trimmed.indexOf('.') : trimmed.indexOf(',');
        if (separatorIdx >= 0) {
            String fraction = trimmed.substring(separatorIdx + 1);
            if (fraction.length() > 2) {
                return ValidationResult.error("Допускается не более двух знаков после запятой.");
            }
        }

        // Normalize decimal separator
        String normalized = trimmed.replace(',', '.');

        // Check for letters or other illegal characters
        // Valid forms: [+-]?digits(\.digits?)? or [+-]?\.digits
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return ValidationResult.error("Сумма должна быть числом.");
        }

        // Must be positive
        if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.error("Сумма должна быть больше нуля.");
        }

        // Must not exceed 1,000,000,000
        if (parsed.compareTo(MAX_AMOUNT) > 0) {
            return ValidationResult.error("Сумма не должна превышать 1 000 000 000.");
        }

        return ValidationResult.success(parsed);
    }

    private static int countOccurrences(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }
}
