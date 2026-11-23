package md.bnm.converter.service;

import md.bnm.converter.model.ConversionResult;
import md.bnm.converter.model.Currency;
import md.bnm.converter.model.PublishedRate;
import md.bnm.converter.model.RateSet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Service for calculating currency conversions through MDL using exact BigDecimal arithmetic.
 * Formulas:
 * - Through-MDL: amount * sourceRate / targetRate
 * - Converted amount rounded to 2 decimal places using HALF_UP
 * - Cross-rate: target units per 1 source unit (sourceRate / targetRate)
 * - Same currency returns entered amount with cross-rate 1
 * - Blocks conversion if a non-MDL currency is missing from rate set
 */
public class CurrencyConverter {

    public static final int INTERMEDIATE_SCALE = 10;
    public static final int DISPLAY_SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public ConversionResult convert(BigDecimal amount, Currency source, Currency target, RateSet rateSet) throws RateLoadException {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(rateSet, "rateSet must not be null");

        // FR-015a: Block conversion if a selected non-MDL currency is absent from the applicable rate set
        PublishedRate sourceRate = rateSet.getRate(source.code());
        if (sourceRate == null) {
            throw new RateLoadException(
                RateLoadErrorType.CURRENCY_NOT_AVAILABLE,
                "Валюта " + source.code() + " отсутствует в актуальном наборе курсов валют."
            );
        }

        PublishedRate targetRate = rateSet.getRate(target.code());
        if (targetRate == null) {
            throw new RateLoadException(
                RateLoadErrorType.CURRENCY_NOT_AVAILABLE,
                "Валюта " + target.code() + " отсутствует в актуальном наборе курсов валют."
            );
        }

        // FR-005: Same-currency conversion returns the entered amount
        if (source.code().equalsIgnoreCase(target.code())) {
            BigDecimal convertedAmount = amount.setScale(DISPLAY_SCALE, ROUNDING_MODE);
            BigDecimal crossRate = BigDecimal.ONE;

            return new ConversionResult(
                amount,
                source,
                target,
                convertedAmount,
                crossRate,
                rateSet.requestedDate(),
                rateSet.applicableDate(),
                ConversionResult.DEFAULT_SOURCE_ATTRIBUTION,
                rateSet.origin(),
                rateSet.explanation()
            );
        }

        // Through-MDL cross rate: sourceRate / targetRate
        BigDecimal sourcePerUnit = sourceRate.ratePerUnit();
        BigDecimal targetPerUnit = targetRate.ratePerUnit();

        BigDecimal crossRate = sourcePerUnit.divide(targetPerUnit, INTERMEDIATE_SCALE, ROUNDING_MODE);

        // Converted amount: amount * sourcePerUnit / targetPerUnit
        BigDecimal convertedUnrounded = amount.multiply(sourcePerUnit).divide(targetPerUnit, INTERMEDIATE_SCALE, ROUNDING_MODE);
        BigDecimal convertedAmount = convertedUnrounded.setScale(DISPLAY_SCALE, ROUNDING_MODE);

        return new ConversionResult(
            amount,
            source,
            target,
            convertedAmount,
            crossRate,
            rateSet.requestedDate(),
            rateSet.applicableDate(),
            ConversionResult.DEFAULT_SOURCE_ATTRIBUTION,
            rateSet.origin(),
            rateSet.explanation()
        );
    }
}
