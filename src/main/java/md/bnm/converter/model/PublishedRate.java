package md.bnm.converter.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An NBM currency quote containing its currency, nominal quantity, quoted value,
 * and derived value for one currency unit relative to MDL.
 */
public record PublishedRate(
    Currency currency,
    int nominal,
    BigDecimal value,
    BigDecimal ratePerUnit
) {
    public static final int RATE_SCALE = 10;
    public static final RoundingMode RATE_ROUNDING = RoundingMode.HALF_UP;

    public static final PublishedRate MDL_RATE = new PublishedRate(
        Currency.MDL,
        1,
        BigDecimal.ONE,
        BigDecimal.ONE.setScale(RATE_SCALE, RATE_ROUNDING)
    );

    public PublishedRate {
        Objects.requireNonNull(currency, "Currency must not be null");
        Objects.requireNonNull(value, "Value must not be null");
        if (nominal <= 0) {
            throw new IllegalArgumentException("Nominal must be a positive integer, got: " + nominal);
        }
        Objects.requireNonNull(ratePerUnit, "ratePerUnit must not be null");
    }

    public PublishedRate(Currency currency, int nominal, BigDecimal value) {
        this(
            currency,
            nominal,
            value,
            calculateRatePerUnit(nominal, value)
        );
    }

    public static BigDecimal calculateRatePerUnit(int nominal, BigDecimal value) {
        Objects.requireNonNull(value, "Value must not be null");
        if (nominal <= 0) {
            throw new IllegalArgumentException("Nominal must be a positive integer, got: " + nominal);
        }
        return value.divide(BigDecimal.valueOf(nominal), RATE_SCALE, RATE_ROUNDING);
    }

    public String currencyCode() {
        return currency.code();
    }
}
