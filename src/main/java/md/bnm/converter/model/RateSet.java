package md.bnm.converter.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The currencies and rates available for a conversion, with provenance and date context.
 * Invariant: rates is an immutable lookup by currency code, always containing MDL.
 */
public record RateSet(
    LocalDate requestedDate,
    LocalDate applicableDate,
    Map<String, PublishedRate> rates,
    RateOrigin origin,
    String explanation
) {
    public RateSet {
        Objects.requireNonNull(requestedDate, "requestedDate must not be null");
        Objects.requireNonNull(applicableDate, "applicableDate must not be null");
        Objects.requireNonNull(origin, "origin must not be null");

        Map<String, PublishedRate> map = new LinkedHashMap<>();
        if (rates != null) {
            map.putAll(rates);
        }
        // Invariant: MDL is always present with a per-unit rate of 1
        if (!map.containsKey(Currency.MDL.code())) {
            map.put(Currency.MDL.code(), PublishedRate.MDL_RATE);
        }
        rates = Collections.unmodifiableMap(map);
    }

    public Optional<String> explanationOpt() {
        return Optional.ofNullable(explanation);
    }

    public PublishedRate getRate(String currencyCode) {
        if (currencyCode == null) {
            return null;
        }
        return rates.get(currencyCode.toUpperCase());
    }

    public boolean hasCurrency(String currencyCode) {
        if (currencyCode == null) {
            return false;
        }
        return rates.containsKey(currencyCode.toUpperCase());
    }
}
