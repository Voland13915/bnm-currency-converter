# Feature Specification: Desktop Currency Converter

**Feature Branch**: `Not created`

**Created**: 2026-11-23

**Status**: Draft

**Input**: User description: Build a desktop currency converter using official National Bank of Moldova exchange rates, with currency/date selection, conversion, swapping, validation, weekend and missing-rate handling, and persistent offline cache.

## Clarifications

### Session 2026-11-23

- Q: What should happen when the selected currency is missing from the applicable rate set? → A: Block conversion and show an unavailable-currency message.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Convert an amount (Priority: P1)

The user enters a positive amount, selects source and target currencies, chooses a rate date (today by default), and converts. The result identifies the converted amount, exchange rate, rate date, and National Bank of Moldova as the source.

**Why this priority**: Conversion is the primary purpose of the application and delivers its core value independently.

**Independent Test**: Select two different currencies and a date with available rates, convert a valid amount, and verify the result and its rate/source/date details.

**Acceptance Scenarios**:

1. **Given** a valid amount, two selected currencies, and a selected date with rates, **When** the user chooses Convert, **Then** the app displays the converted amount to two decimal places, the rate used to up to four decimal places, the applicable rate date, and National Bank of Moldova as the source.
2. **Given** one or more required fields are empty, **When** the user views the form, **Then** Convert is disabled.
3. **Given** an amount is empty, contains letters, is zero or negative, or has more than two decimal places, **When** the user attempts conversion, **Then** an error appears next to the amount field and no result is produced.
4. **Given** a valid amount uses a comma decimal separator, **When** the user converts, **Then** the app accepts it and calculates the same value as for a period separator.
5. **Given** the same currency is selected as source and target, **When** the user converts a valid amount, **Then** the result equals the entered amount and no error is shown.
6. **Given** the selected date is Saturday or Sunday, **When** the user converts, **Then** the app shows the applicable rate and explains that NBM does not set rates on weekends and the previous Friday's rate applies.
7. **Given** the selected date's response contains no currencies, **When** earlier rates are available within the preceding 10 days, **Then** the app uses the most recent earlier date containing rates and tells the user the actual date and that an earlier rate was used.
8. **Given** no earlier date with rates is found within 10 days, **When** the user requests conversion, **Then** the app shows a clear no-rates-available message and produces no conversion.
9. **Given** a rate date is in the future, **When** the user chooses a date, **Then** future dates are unavailable for selection.

---

### User Story 2 - Swap currency direction (Priority: P2)

The user swaps the selected source and target currencies with one action to convert in the opposite direction.

**Why this priority**: Swapping makes reverse conversion immediate while remaining independent of the primary conversion flow.

**Independent Test**: Select two different currencies, activate Swap, and verify that the source and target selections exchange places.

**Acceptance Scenarios**:

1. **Given** source and target currencies are selected, **When** the user chooses Swap, **Then** the former source becomes the target and the former target becomes the source.
2. **Given** the user has swapped currencies, **When** the user converts the same amount using the same applicable rates, **Then** the result reflects the reverse direction and returns the original amount within displayed rounding.

---

### User Story 3 - Continue when the source is unavailable (Priority: P3)

When current rates cannot be reached, the user receives a clear explanation and can choose to use the last successfully loaded rates saved locally, including after restarting the application.

**Why this priority**: Cached rates allow a useful conversion when the NBM source is temporarily unavailable, without obscuring the data's age or origin.

**Independent Test**: Save a successful rate response, restart the app, make the source unavailable, and verify that the app offers cached rates and identifies their date and local-cache status.

**Acceptance Scenarios**:

1. **Given** the rate source cannot be reached and a successful response was previously saved, **When** the user requests a conversion, **Then** the app gives a plain-language source-unavailable message and offers the saved rates for use.
2. **Given** the user accepts the cached rates, **When** conversion completes, **Then** the result identifies the cached rate date and states that the rates came from the local cache.
3. **Given** the rate source cannot be reached and no successful response was saved, **When** the user requests a conversion, **Then** the app shows a clear unavailable-rates message and remains usable without producing a result.
4. **Given** the source returns a timeout, server error, empty rate response, or malformed response, **When** the app processes it, **Then** it shows a short plain-language message identifying the type of problem and does not crash.

### Edge Cases

- NBM quotes some rates for multiple units; the rate for one currency unit is the quoted value divided by its nominal.
- MDL is available as a currency with a rate of 1 even though it is absent from the NBM currency response.
- Conversions between any two currencies are calculated through MDL, so reversing the direction returns the original amount within rounding to two decimal places.
- If neither selected currency is MDL and either currency is missing from the applicable rate set, the application must block conversion and show an unavailable-currency message.
- The NBM response's root date on a weekend is the requested date, not proof that a rate was published that day; the app must use the weekend rule rather than claiming that a weekend rate was set.
- The user may change currency or date selections after a previous result; any subsequent result must identify the selections and rate data used for that conversion.
- Network, timeout, server, empty-response, malformed-response, and unavailable-cache cases must leave the application usable and must not display a success-shaped result.
- Real NBM responses for a business day and a weekend day are retained unmodified as verification examples in `src/test/resources`, preserving their response format.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The application MUST provide a graphical desktop interface for amount entry, source and target currency selection, rate-date selection, conversion, and direction swapping.
- **FR-002**: The application MUST list every currency present in the NBM rate response and Moldovan leu (MDL).
- **FR-003**: The application MUST treat MDL as having a rate of 1 and MUST calculate each quoted currency's one-unit rate by dividing its quoted value by its nominal.
- **FR-004**: The application MUST calculate conversions through MDL using amount × source rate ÷ target rate.
- **FR-005**: The application MUST allow the same currency in both selections and return the entered amount as the result.
- **FR-006**: The rate date MUST default to today, and the user MUST NOT be able to select a future date.
- **FR-007**: The Convert action MUST remain disabled until the amount, source currency, target currency, and rate date are filled in.
- **FR-008**: The application MUST accept period and comma as decimal separators and MUST allow no more than two digits after the decimal separator.
- **FR-009**: An empty, non-numeric, zero, negative, or over-precision amount MUST produce a field-level error and no conversion.
- **FR-010**: The application MUST show converted amounts to two decimal places and rates to no more than four decimal places.
- **FR-011**: Each conversion result MUST identify the exchange rate as target-currency units per one source-currency unit, the applicable rate date, and National Bank of Moldova as the source.
- **FR-012**: The Swap action MUST exchange the selected source and target currencies.
- **FR-013**: For a Saturday or Sunday selection, the application MUST state that NBM does not set rates on weekends and that the previous Friday's rate applies.
- **FR-014**: If the requested date's response contains no currencies, the application MUST search earlier dates up to 10 days back, use the most recent earlier date containing rates if found, and identify the actual date and reason for using it.
- **FR-015**: If no rates are found within that 10-day search, the application MUST report that rates are unavailable and MUST NOT produce a conversion.
- **FR-015a**: If a selected non-MDL currency is absent from the applicable rate set, the application MUST block conversion and show an unavailable-currency message.
- **FR-016**: The application MUST save the last successful NBM response locally so it remains available after an application restart.
- **FR-017**: When the rate source cannot be reached and a saved response exists, the application MUST offer the user the cached rates. If accepted, results MUST identify the cached date and local-cache origin.
- **FR-018**: If the rate source cannot be reached and no saved response exists, the application MUST display a clear message and remain usable without producing a conversion.
- **FR-019**: The application MUST report network unavailability, timeout, server error, empty response, and malformed response with short plain-language messages, without crashing.
- **FR-020**: Verification materials MUST include real NBM response examples for a business day and a weekend day, preserving the original response format in `src/test/resources`.

### Key Entities

- **Currency**: A selectable currency, identified by its code and display name, with MDL represented at a rate of 1.
- **Published rate**: The value and nominal for one currency relative to MDL, together with the date on which the rate applies.
- **Rate set**: The currencies and rates available for an applicable date, including whether the set came from the current source, an earlier-date fallback, or local cache.
- **Conversion result**: The entered amount, source and target currencies, converted amount, rate used, applicable rate date, and source/fallback explanation.
- **Cached response**: The most recent successful NBM response saved for use across application restarts.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In all defined conversion checks, results match the stated through-MDL calculation and are displayed rounded to two decimal places; reversing a conversion recovers the original amount within two-decimal-place rounding.
- **SC-002**: Users can complete a conversion with valid selections in one Convert action, and every result includes the rate, applicable date, and NBM source.
- **SC-003**: Every invalid amount in the specified validation cases is blocked from conversion and receives an adjacent error message.
- **SC-004**: Weekend dates, dates with no published rates, and missing rates within the 10-day search produce the corresponding required explanation or unavailable-rates message.
- **SC-005**: After a successful rate load and application restart, an unavailable source can be handled using the saved response, with its date and cache origin clearly identified.
- **SC-006**: Each specified source failure case produces a plain-language error while leaving the application available for further use.
- **SC-007**: When rate data is already available, at least 95% of conversion requests display a result within two seconds of the user choosing Convert.

## Assumptions

- The interface is intended for one user at a time and does not require user accounts or online transaction services.
- Amount entry uses a period or comma decimal separator and supports at most two fractional digits, as specified.
- The cache retains the last successful NBM response; cache expiration and historical-rate archival beyond the requested-date fallback are outside this feature's scope.
- If cached rates are offered after the source becomes unavailable, the user chooses whether to proceed with them.
- The displayed rate is the cross-rate expressed as target-currency units per one source-currency unit; result display time is measured after usable rate data is available.
- The application displays rates as supplied by NBM and does not provide financial advice or guarantee that rates represent a transaction price.
