package md.bnm.converter.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable record of one completed conversion.
 */
public record ConversionResult(
    BigDecimal amount,
    Currency sourceCurrency,
    Currency targetCurrency,
    BigDecimal convertedAmount,
    BigDecimal crossRate,
    LocalDate requestedDate,
    LocalDate applicableDate,
    String sourceAttribution,
    RateOrigin origin,
    String explanation
) {
    public static final String DEFAULT_SOURCE_ATTRIBUTION = "Национальный банк Молдовы";

    public ConversionResult {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(sourceCurrency, "sourceCurrency must not be null");
        Objects.requireNonNull(targetCurrency, "targetCurrency must not be null");
        Objects.requireNonNull(convertedAmount, "convertedAmount must not be null");
        Objects.requireNonNull(crossRate, "crossRate must not be null");
        Objects.requireNonNull(requestedDate, "requestedDate must not be null");
        Objects.requireNonNull(applicableDate, "applicableDate must not be null");
        Objects.requireNonNull(sourceAttribution, "sourceAttribution must not be null");
        Objects.requireNonNull(origin, "origin must not be null");
    }

    public Optional<String> explanationOpt() {
        return Optional.ofNullable(explanation);
    }
}
