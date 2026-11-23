package md.bnm.converter.service;

import md.bnm.converter.model.ConversionResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Formatter for conversion results according to FR-010, FR-011, and data-model.md specifications.
 * Displays converted amounts to 2 decimal places, cross-rates to at most 4 decimal places,
 * applicable rate date, source attribution, and explanatory notes.
 */
public class ConversionResultFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String formatRate(BigDecimal rate) {
        if (rate == null) {
            return "";
        }
        BigDecimal rounded = rate.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
        // Ensure at least .0 is displayed if an integer, or plain string
        String plain = rounded.toPlainString();
        return plain.contains(".") ? plain : plain + ".0";
    }

    public static String formatConversionSummary(ConversionResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return formatAmount(result.amount()) + " " + result.sourceCurrency().code()
            + " = "
            + formatAmount(result.convertedAmount()) + " " + result.targetCurrency().code();
    }

    public static String formatRateDetails(ConversionResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return "1 " + result.sourceCurrency().code()
            + " = "
            + formatRate(result.crossRate()) + " " + result.targetCurrency().code();
    }

    public static String formatSourceAndDate(ConversionResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return "Дата курса: " + result.applicableDate().format(DATE_FORMATTER)
            + " • Источник: " + result.sourceAttribution();
    }

    public static String formatFullResult(ConversionResult result) {
        Objects.requireNonNull(result, "result must not be null");
        StringBuilder sb = new StringBuilder();
        sb.append(formatConversionSummary(result)).append("\n");
        sb.append("Курс: ").append(formatRateDetails(result)).append("\n");
        sb.append(formatSourceAndDate(result));

        if (result.explanation() != null && !result.explanation().isBlank()) {
            sb.append("\nПримечание: ").append(result.explanation());
        }

        return sb.toString();
    }
}
