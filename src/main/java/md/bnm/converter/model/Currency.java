package md.bnm.converter.model;

import java.util.Objects;

/**
 * Immutable currency representation with a stable three-letter code, display name,
 * and optional NBM numeric code.
 * MDL is always present with a per-unit rate of 1.
 */
public record Currency(String code, String displayName, String numCode) {

    public static final Currency MDL = new Currency("MDL", "Молдавский лей", "498");

    public Currency {
        Objects.requireNonNull(code, "Currency code must not be null");
        Objects.requireNonNull(displayName, "Display name must not be null");
        if (code.isBlank()) {
            throw new IllegalArgumentException("Currency code must not be blank");
        }
    }

    public Currency(String code, String displayName) {
        this(code, displayName, null);
    }
}
