# Research: Desktop Currency Converter

## Java 21 and Maven

- **Decision**: Use Java 21 and the standard Maven application layout.
- **Rationale**: These are explicit project constraints and support the requested JavaFX desktop application and JUnit workflow.
- **Alternatives considered**: Other Java versions and build tools; rejected because they conflict with the required platform.

## JavaFX 21 and Background Work

- **Decision**: Use JavaFX 21 for the Russian desktop UI; perform HTTP and XML work in `javafx.concurrent.Task` and update controls on the JavaFX application thread.
- **Rationale**: Network requests can take up to 10 seconds, so they must not block UI interaction. A Task provides success, failure, and running-state hooks for loading and error feedback.
- **Alternatives considered**: Synchronous UI-thread requests; rejected because a slow request would freeze the window. A separate UI toolkit; rejected because JavaFX is specified.

## HTTP Retrieval

- **Decision**: Use JDK `java.net.http.HttpClient` with the NBM XML endpoint and a 10-second request timeout.
- **Rationale**: The JDK client avoids a third-party HTTP dependency and provides request timeout and HTTP status handling.
- **Alternatives considered**: Third-party HTTP clients; unnecessary for this single GET integration and inconsistent with the minimal-dependency constraint.

## XML Parsing

- **Decision**: Use the JDK XML parser with DTD and external entity processing disabled; validate the root date and required currency fields.
- **Rationale**: The NBM source is XML and the project requires JDK XML APIs. Disabling external processing prevents the parser from resolving untrusted external content. Malformed data remains distinct from a valid response with zero currencies.
- **Alternatives considered**: A third-party XML library; not needed for this response shape and would add a dependency.

## Rates, Dates, and Fallback

- **Decision**: Parse values and nominal amounts as `BigDecimal`, derive one-unit rates at scale 10 with `HALF_UP`, and use MDL as the cross-rate base. When a valid response has no currencies, try each prior calendar date from D-1 through D-10.
- **Rationale**: This preserves exact decimal arithmetic and follows the requested NBM behavior. Weekend handling is based on the selected date, not the response's root date; weekend results must describe the previous Friday's rate.
- **Alternatives considered**: Floating-point calculations; rejected by the constitution. Treating malformed XML as an empty date; rejected because it can conceal source corruption.

## Persistent Cache

- **Decision**: Save the last successful raw XML response in `~/.bnm-converter/` and reparse it using the same parser. Offer cached rates after a live-source failure rather than using them silently.
- **Rationale**: This survives application restarts, avoids a second cache schema, and preserves the source payload. A user choice and visible cache date maintain traceability.
- **Alternatives considered**: An in-memory-only cache; rejected because it does not survive restart. A separate serialized rates format; unnecessary when the raw response is available.

## Testing

- **Decision**: Use JUnit 5 with `mvn test`; preserve the existing real weekday/weekend XML examples and add empty and malformed fixtures. Keep test implementation in the final test phase.
- **Rationale**: This matches the constitution and validates both source-specific parsing and domain behavior without requiring JavaFX for unit tests.
- **Alternatives considered**: Manual-only verification or another test framework; rejected because automated unit coverage is required and JUnit 5 is specified.

## Known Inputs and Remaining Unknowns

The endpoint, response fields, decimal syntax, amount range, date behavior, cache location, timeout, arithmetic, and stack are provided by the feature specification and technical plan. No unresolved technical choices remain. The existing weekday fixture is dated 20 November 2026 and the weekend fixture 22 November 2026; their equal sample rates are consistent with displaying Friday's rates for a weekend request.

## References

- [Java 21 `HttpClient` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html) — JDK HTTP requests and response handling.
- [JavaFX 21 `Task` API](https://openjfx.io/javadoc/21/javafx.graphics/javafx/concurrent/Task.html) — background work and observable task state.
- [Java 21 JAXP Security Guide](https://docs.oracle.com/en/java/javase/21/security/java-api-xml-processing-jaxp-security-guide.html) — XML external-entity and external-resource risks.
- [JUnit 5.11.4 User Guide](https://junit.org/junit5/docs/5.11.4/user-guide/) — JUnit 5 testing and Maven support.
