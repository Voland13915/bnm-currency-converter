# Implementation Plan: Desktop Currency Converter

**Branch**: `001-desktop-currency-converter` | **Date**: 2026-11-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-desktop-currency-converter/spec.md` and the user-provided technical plan.

## Summary

Build a Russian-language JavaFX desktop currency converter that retrieves official NBM XML rates for the selected date, converts through MDL using `BigDecimal`, and reports the date and source of every result. Keep HTTP, XML parsing, caching, conversion, and validation in Java classes independent of JavaFX. Retrieve rates asynchronously, search earlier dates only when a response contains no currencies, and offer the persistent last-successful response when live rates cannot be used.

## Technical Context

- **Language/Version**: Java 21
- **Primary Dependencies**: JavaFX 21 (OpenJFX from Maven Central), `javafx-maven-plugin` for `mvn javafx:run`, JUnit 5. HTTP and XML use JDK standard-library APIs.
- **Storage**: Raw successful NBM XML at `~/.bnm-converter/`; response date is retained in the XML root `Date` attribute.
- **Testing**: JUnit 5 with `mvn test`; real business-day and weekend XML fixtures are already present in `src/test/resources`. Add focused empty and malformed response fixtures.
- **Target Platform**: Desktop systems supported by Java 21 and JavaFX 21.
- **Project Type**: Single Maven desktop application.
- **Performance Goals**: With rates already loaded, at least 95% of conversions display within two seconds; conversion itself is local.
- **Constraints**: HTTP request timeout of 10 seconds; network work must not block the JavaFX thread; amount is positive and at most 1,000,000,000 with no more than two fractional digits; results use two decimal places (`HALF_UP`), and per-unit rates use scale 10 (`HALF_UP`). No future rate dates.
- **Scale/Scope**: One desktop user, one conversion window, and the currencies in an NBM response plus MDL. Rates load at startup for today and on date changes, with no duplicate request for a date already loaded in the session.

## Constitution Check

| Principle | Gate | Plan |
|---|---|---|
| I. Exact Monetary Arithmetic | PASS | Use `BigDecimal` throughout; retain quoted precision, divide by nominal at scale 10 with `HALF_UP`, and round converted display amounts to two places with `HALF_UP`. |
| II. Resilient User Experience | PASS | Distinguish timeout, network/HTTP, empty-rate, malformed-response, cache, and unavailable-currency states; present concise Russian messages and keep the window usable. |
| III. Traceable Results | PASS | Results include the requested and applicable rate dates, NBM source, and fallback/cache explanation. |
| IV. Offline-First Data | PASS | Persist the last successful raw XML response and offer its parsed rates after a live-source failure. |
| V. UI-Independent Domain Logic | PASS | Only `ui` depends on JavaFX; source, XML, cache, service, model, conversion, and validation code remain plain Java. |
| VI. Behavior Is Covered by Tests | PASS | Focused JUnit 5 tests are planned for parsing, empty/malformed data, conversions, and validation, run with `mvn test`. |
| VII. Minimal, Approved Technology | PASS | Use the requested Java 21, JavaFX 21, Maven, and JUnit 5 stack. HTTP/XML use JDK APIs; JavaFX and its Maven plugin are required by the specified desktop UI and launch workflow. |
| VIII. Specification Is Authoritative | PASS | Preserve the spec behavior, including the weekend explanation, 10-day empty-rate search, cache choice, and Russian interface. |

No constitution violations or unresolved technical choices remain.

## Project Structure

### Documentation (this feature)

```text
specs/001-desktop-currency-converter/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── desktop-ui.md
```

### Source Code (repository root)

```text
pom.xml
src/
├── main/
│   ├── java/md/bnm/converter/
│   │   ├── model/       # immutable currency, rate, and result records
│   │   ├── source/      # HTTP client and secure XML parser
│   │   ├── cache/       # persistent raw-response cache
│   │   ├── service/     # rate provider/fallback, converter, validator
│   │   └── ui/          # JavaFX window and asynchronous UI tasks
│   └── resources/
└── test/
    ├── java/md/bnm/converter/
    └── resources/
        ├── bnm-weekday.xml
        ├── bnm-weekend.xml
        ├── bnm-empty.xml
        └── bnm-malformed.xml
```

**Structure Decision**: Use standard Maven single-project layout. Match packages to the requested separation of models, source I/O, cache, services, and JavaFX UI. The test resources directory already contains the real weekday and weekend examples; add synthetic empty and malformed fixtures without modifying the real examples.

## Post-Design Constitution Recheck

**PASS**: The design preserves exact decimal arithmetic and traceable dates/origins; keeps source, parsing, cache, and domain logic independent of JavaFX; disables external XML processing; makes source/cache failures explicit; and adds no dependencies beyond the approved JavaFX/Maven/JUnit stack. The planned unit tests remain runnable with `mvn test`.

## Design Decisions

- Request `https://www.bnm.md/en/official_exchange_rates?get_xml=1&date=DD.MM.YYYY` with JDK `HttpClient` and a 10-second timeout. Execute retrieval and parsing in a JavaFX `Task`; expose loading and failure states on the UI thread.
- Configure the JDK XML parser to disallow DTDs and external entities. Parse the root `Date` and all required `Valute` fields into immutable model records. No `Valute` entries means no rates for that date and triggers earlier-date search; invalid XML or missing required fields is a malformed-response error.
- Load on startup and each selected-date change. Track dates loaded during the session to avoid duplicate HTTP requests.
- Search D-1 through D-10 only when a valid response has no currencies. Use the first earlier non-empty rate set; if none exists, report unavailable rates. Do not treat malformed XML as an empty-rate result.
- Determine weekend treatment from the requested date. For Saturday/Sunday, describe the rates as the previous Friday's rates even if the response root date reflects the requested weekend date. Keep the requested date distinct from the effective/applicable date.
- Store the last successful raw response under `~/.bnm-converter/`. Reparse it with the same parser; ignore a corrupted cache with a warning. On live-source failure, offer the cached response for explicit user acceptance and identify its date and local-cache origin if used.
- Always include MDL at rate 1. Compute quoted per-unit MDL rates as `Value / Nominal` at scale 10, `HALF_UP`; convert through MDL as `amount × sourceRate / targetRate`. Block conversion if a selected non-MDL currency is absent.
- Validate input independently from JavaFX. Accept comma or period as the decimal separator (not both), with at most two fractional digits, and enforce `0 < amount ≤ 1,000,000,000`.
- Keep implementation phases separate from the final test-writing phase as requested; tests are still part of the planned deliverable and run through `mvn test`.

## Complexity Tracking

No constitution violations requiring justification.
