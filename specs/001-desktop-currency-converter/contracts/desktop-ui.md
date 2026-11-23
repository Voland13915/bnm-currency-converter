# Desktop UI Contract

The application exposes a Russian-language desktop interface; it does not expose a network API or command-line interface.

## Controls and Defaults

- Amount field accepts period or comma decimal notation.
- Source and target selectors contain every currency from the available NBM rate set plus MDL.
- Date selector defaults to today and disallows future dates.
- Convert stays disabled until amount, both currencies, and date are present and rates have loaded. Invalid amounts remain eligible for submission so they can receive a field-level validation error.
- Swap exchanges source and target selections.
- A loading indicator communicates asynchronous rate retrieval.

## Conversion

- Invalid amounts show an adjacent field error; no result is produced.
- If a selected non-MDL currency is absent from the applicable rate set, conversion is blocked with an unavailable-currency message.
- A successful result displays the amount to two decimal places, target-currency units per one source-currency unit to no more than four decimal places, applicable rate date, and National Bank of Moldova.
- Same-currency conversion returns the entered amount.
- Weekend results explain in Russian that NBM does not set rates on weekends and the previous Friday's rate applies.
- Earlier-date fallback and accepted cache use are identified next to the result, including the actual rate date and cache origin as applicable.

## Rate Loading and Failures

- Load rates at startup for today and whenever the selected date changes; do not request the same date twice in one session.
- A valid response with no `Valute` elements triggers requests for earlier dates, D-1 through D-10. Use the most recent earlier date containing rates or show that rates are unavailable.
- Malformed XML and missing required fields are errors, not empty-rate results.
- Network, timeout, HTTP/server, empty-rate, and malformed-response conditions show concise Russian messages and leave the window usable.
- When live rates cannot be used, offer the last successful cached response if it can be parsed. Use it only after the user accepts; identify its date and local-cache status. If no valid cache is available or the user declines, do not produce a conversion.
