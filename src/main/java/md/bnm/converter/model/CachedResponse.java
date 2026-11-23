package md.bnm.converter.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * An immutable record representing the last successfully parsed NBM source response,
 * persisted as raw XML.
 */
public record CachedResponse(
    LocalDate date,
    String rawXml
) {
    public CachedResponse {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(rawXml, "rawXml must not be null");
        if (rawXml.isBlank()) {
            throw new IllegalArgumentException("rawXml must not be blank");
        }
    }
}
