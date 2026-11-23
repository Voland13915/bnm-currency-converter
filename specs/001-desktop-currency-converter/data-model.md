# Data Model: Desktop Currency Converter

## Currency

Selectable currency represented by a stable three-letter code and display name. Preserve the NBM numeric code when supplied. MDL is always present with a per-unit rate of 1, even though it is not included in the NBM `Valute` list.

## Published Rate

An NBM currency quote containing its currency, nominal quantity, quoted `Value`, and derived value for one currency unit relative to MDL.

- `Nominal` must be a positive integer.
- `Value` must parse as a decimal number.
- Per-unit rate is `Value / Nominal`, calculated with `BigDecimal` at scale 10 using `HALF_UP`.
- Currency code is unique within a rate set.

## Rate Set

The currencies and rates available for a conversion, with provenance and date context.

- `requestedDate`: date selected by the user.
- `applicableDate`: applicable date of the rates, distinct from the selected date when weekend or empty-date fallback rules apply.
- `rates`: immutable lookup by currency code, always containing MDL.
- `origin`: live response, earlier-date fallback, or accepted local cache.
- `explanation`: optional user-facing reason for weekend, fallback, or cache use.

For Saturday and Sunday, the weekend explanation states that the previous Friday's rates apply, regardless of the response root date. For an empty requested-date response, the applicable date is the first earlier date with rates among D-1 through D-10. A valid response with no rates across that range yields no rate set.

## Conversion Result

An immutable record of one completed conversion.

- Original positive amount and source/target currencies.
- Converted amount rounded to two decimal places using `HALF_UP`.
- Cross-rate expressed as target-currency units per one source-currency unit.
- Requested and applicable dates, plus NBM source attribution.
- Rate origin and the applicable fallback/cache explanation.

For currencies with rates `sourceRate` and `targetRate` against MDL, the unrounded converted amount is `amount × sourceRate / targetRate`. Same-currency conversions return the entered amount.

## Cached Response

The last successfully parsed NBM source response, persisted as raw XML under `~/.bnm-converter/`. Its root `Date` and currency data are read with the regular parser. A parse failure marks the cache unusable and produces a warning; corrupted cache data is not used as rates.

## Amount Validation

Input validation returns either a parsed `BigDecimal` amount or a field-level error.

- Accept period or comma as the decimal separator.
- Allow at most two fractional digits.
- Require a value greater than zero and at most 1,000,000,000.
- Reject empty, non-numeric, negative/zero, over-precision, and above-maximum values.

## UI Loading State

The UI tracks the selected date's loading/ready/error state and whether it has already been requested in the current session. While loading, show progress and prevent conversion without blocking the JavaFX thread. A source failure may transition to an explicit cached-rate offer; accepting it marks the resulting rate set as cache-origin.
