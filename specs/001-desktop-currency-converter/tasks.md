# Tasks: Desktop Currency Converter

**Input**: Design documents from `/specs/001-desktop-currency-converter/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: JUnit 5 unit coverage is required by the constitution and quickstart for parsing, conversion, validation, and fallback behavior.

**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize the Maven Java 21 and JavaFX 21 application.

- [X] T001 Create the Maven project structure and `pom.xml` with Java 21, JavaFX 21, the JavaFX Maven plugin, JUnit 5, and Maven test and JavaFX run configuration at repository root
- [X] T002 [P] Create package directories for `src/main/java/md/bnm/converter/{model,source,cache,service,ui}` and `src/test/java/md/bnm/converter/{model,source,cache,service,ui}` in the repository
- [X] T003 [P] Add empty and malformed XML fixtures in `src/test/resources/bnm-empty.xml` and `src/test/resources/bnm-malformed.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core types and infrastructure that every user story requires.

**CRITICAL**: No user-story implementation can begin until this phase is complete.

- [X] T004 Create immutable `Currency`, `PublishedRate`, `RateSet`, `ConversionResult`, and `CachedResponse` records in `src/main/java/md/bnm/converter/model/` with fields and invariants from `data-model.md` (cite `data-model.md` `Currency.code`, `Currency.displayName`, `Currency.MDL rate of 1`, and `Rate Set.rates`)
- [X] T005 [P] Implement secure JAXP XML parsing and root-date/rate validation in `src/main/java/md/bnm/converter/source/NbmXmlParser.java`
- [X] T006 [P] Implement NBM HTTP retrieval with JDK `HttpClient`, a 10-second timeout, status handling, and live-result errors in `src/main/java/md/bnm/converter/source/NbmRateSource.java`
- [X] T007 [P] Implement persistent raw-XML cache read/write and corrupted-cache handling in `src/main/java/md/bnm/converter/cache/ResponseCache.java`
- [X] T008 Implement shared source/result exceptions and error-state types in `src/main/java/md/bnm/converter/service/RateLoadException.java`
- [X] T009 Implement a JavaFX-independent rate provider that tracks loaded dates, searches D-1 through D-10 for empty responses, and applies weekend context in `src/main/java/md/bnm/converter/service/RateProvider.java` (cite `data-model.md` `Rate Set.rates`)

**Checkpoint**: Foundation ready; exact domain types, source parsing, cache, and rate loading are available.

---

## Phase 3: User Story 1 - Convert an amount (Priority: P1) MVP

**Goal**: Provide exact through-MDL conversion with Russian validation and traceable result details.

**Independent Test**: Select available currencies and a dated rate set, enter valid and invalid amounts, and verify conversion, validation, weekend, fallback, and missing-currency behavior.

### Implementation for User Story 1

- [X] T010 [P] [US1] Implement amount validation for `data-model.md` `Amount Validation`: period or comma separator, at most two fractional digits, `> 0`, and maximum 1,000,000,000 in `src/main/java/md/bnm/converter/service/AmountValidator.java`
- [X] T011 [US1] Implement through-MDL conversion and unavailable-currency checks in `src/main/java/md/bnm/converter/service/CurrencyConverter.java`, using `data-model.md` `Published Rate.Per-unit rate` and `Conversion Result.Converted amount`
- [X] T012 [US1] Implement conversion result formatting for two-decimal amounts, cross-rates to no more than four decimals, requested/applicable dates, NBM source, and fallback text in `src/main/java/md/bnm/converter/service/ConversionResultFormatter.java`
- [X] T013 [US1] Build the Russian JavaFX form for amount, currency selectors, date selection, result/error areas, and future-date prohibition in `src/main/java/md/bnm/converter/ui/CurrencyConverterView.java`
- [X] T014 [US1] Implement JavaFX `Task`-based startup and date-change loading plus session-date deduplication in `src/main/java/md/bnm/converter/ui/RateLoadingController.java`
- [X] T015 [US1] Wire Convert to validation, loaded-rate checks, conversion, and Russian field-level errors in `src/main/java/md/bnm/converter/ui/CurrencyConverterController.java`
- [X] T016 [US1] Add Russian weekend and earlier-date fallback explanations to `src/main/java/md/bnm/converter/ui/CurrencyConverterController.java` using `data-model.md` `Rate Set.explanation`
- [X] T017 [US1] Create the JavaFX application entry point in `src/main/java/md/bnm/converter/ui/ConverterApplication.java`

**Checkpoint**: User Story 1 works independently: valid conversion and all specified amount/rate-date/missing-currency outcomes are testable.

---

## Phase 4: User Story 2 - Swap currency direction (Priority: P2)

**Goal**: Let the user reverse source and target immediately and preserve traceable conversion behavior.

**Independent Test**: Select two currencies, activate Swap, and verify the selectors change and reverse conversion is correct.

### Implementation for User Story 2

- [X] T018 [US2] Implement Swap behavior in `src/main/java/md/bnm/converter/ui/CurrencyConverterController.java`
- [X] T019 [US2] Update result rendering after Swap in `src/main/java/md/bnm/converter/ui/CurrencyConverterView.java`

**Checkpoint**: User Stories 1 and 2 work independently; swapping reverses selections and recalculates through MDL.

---

## Phase 5: User Story 3 - Continue when the source is unavailable (Priority: P3)

**Goal**: Offer persistent cached rates after live-source failures without producing success-shaped or untraceable results.

**Independent Test**: Save a successful response, restart, make the source unavailable, and verify explicit cache acceptance and cache-date/origin traceability.

### Implementation for User Story 3

- [X] T020 [US3] Persist the last successfully parsed raw NBM XML response using `data-model.md` `Cached Response` and its raw XML, root `Date`, and corrupted-cache handling constraints in `src/main/java/md/bnm/converter/cache/ResponseCache.java`
- [X] T021 [US3] Reparse cached XML, reject corrupted cache data, and mark cache origin only after acceptance in `src/main/java/md/bnm/converter/service/RateProvider.java`
- [X] T022 [US3] Add Russian source-failure and explicit cached-rate offer states to `src/main/java/md/bnm/converter/ui/RateLoadingController.java`
- [X] T023 [US3] Wire cached-rate acceptance and rejection into `src/main/java/md/bnm/converter/ui/CurrencyConverterController.java`
- [X] T024 [US3] Add cache date and local-cache origin to successful result rendering in `src/main/java/md/bnm/converter/ui/CurrencyConverterView.java`

**Checkpoint**: All user stories are independently functional, and live/cache failures remain usable and traceable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification, documentation, and robustness across stories.

- [ ] T025 [P] Add Russian error strings and consistent result labels across `src/main/java/md/bnm/converter/ui/`
- [ ] T026 Verify Maven dependencies remain limited to JavaFX and JUnit 5 in `pom.xml`
- [ ] T027 Run `mvn javafx:run` and validate startup, conversion, Swap, fallback, and cache workflows against `specs/001-desktop-currency-converter/quickstart.md`

---

## Phase 7: Unit tests

**Purpose**: Complete JUnit 5 unit coverage in a separate final phase.

- [ ] T028 [P] Add reusable JUnit 5 test configuration and baseline test base classes under `src/test/java/md/bnm/converter/`
- [ ] T029 [P] [US1] Test parsing of real weekday and weekend fixtures plus required `Published Rate` fields in `src/test/java/md/bnm/converter/source/NbmXmlParserTest.java` (cite `data-model.md` `Published Rate.Nominal`, `Published Rate.Value`, and `Published Rate.Currency code`)
- [ ] T030 [P] [US1] Test empty and malformed XML distinction in `src/test/java/md/bnm/converter/source/NbmXmlParserTest.java`
- [ ] T031 [P] [US1] Test period/comma amount parsing and field constraints in `src/test/java/md/bnm/converter/service/AmountValidatorTest.java` (cite `data-model.md` `Amount Validation`: accept period or comma, at most two fractional digits, require greater than zero and at most 1,000,000,000, and reject empty/non-numeric/negative/zero/over-precision/above-maximum values)
- [ ] T032 [P] [US1] Test same-currency and cross-currency `BigDecimal` conversion, four-decimal display, and reverse-direction rounding in `src/test/java/md/bnm/converter/service/CurrencyConverterTest.java` (cite `data-model.md` `Published Rate.Per-unit rate`, `Conversion Result.Converted amount`, and `Conversion Result.Cross-rate`)
- [ ] T033 [P] [US1] Test weekend context and D-1 through D-10 empty-response fallback in `src/test/java/md/bnm/converter/service/RateProviderTest.java` (cite `data-model.md` `Rate Set.requestedDate`, `Rate Set.applicableDate`, and `Rate Set.explanation`)
- [ ] T034 [P] [US1] Test unavailable-currency blocking in `src/test/java/md/bnm/converter/service/CurrencyConverterTest.java`
- [ ] T035 [P] [US2] Test reverse conversion recovery within two-decimal display rounding in `src/test/java/md/bnm/converter/service/CurrencyConverterTest.java`
- [ ] T036 [P] [US3] Test cache writes, restart reloading, and ignored corrupted cache data in `src/test/java/md/bnm/converter/cache/ResponseCacheTest.java` (cite `data-model.md` `Cached Response` and its raw XML, root `Date`, and corrupted-cache handling constraints)
- [ ] T037 [P] [US3] Test timeout, network, HTTP, empty-rate, malformed-response, and no-cache messages in `src/test/java/md/bnm/converter/service/RateProviderTest.java`
- [ ] T038 [P] [US3] Test that cache origin is applied only after explicit acceptance in `src/test/java/md/bnm/converter/service/RateProviderTest.java` (cite `data-model.md` `Rate Set.origin`)
- [ ] T039 [P] Add or complete JUnit 5 tests in `src/test/java/md/bnm/converter/` for all mandatory behaviors, preserving the real fixtures under `src/test/resources/`
- [ ] T040 Run `mvn test` and resolve failures in the repository
- [ ] T041 [P] Document the `mvn test` command in `README.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; starts immediately.
- **Foundational (Phase 2)**: Depends on Setup completion and blocks every user story.
- **User Stories (Phase 3+)**: Depend on Foundational completion.
  - US1, US2, and US3 can proceed independently after Foundation when staffed separately.
  - Sequential P1 -> P2 -> P3 execution preserves priority order.
- **Polish (Phase 6)**: Depends on all selected user stories being complete.
- **Unit tests (Phase 7)**: Follows implementation and Polish and is performed as the final verification phase.

### User Story Dependencies

- **US1 (P1)**: No dependency on US2 or US3.
- **US2 (P2)**: Uses the US1 conversion and UI state but remains independently testable.
- **US3 (P3)**: Uses the shared rate source/cache foundation and US1 result state but remains independently testable.

### Within Each User Story

- Data models precede services.
- Services precede UI integration.
- Core implementation precedes fallback/cache result integration.
- Complete and validate one story checkpoint before its priority successor if not running in parallel.

---

## Parallel Opportunities

- T002-T003 can run after T001.
- T005-T007 can run concurrently after model types in T004.
- US1 implementation tasks T010-T017 can run across separate files after model-independent analysis.
- US2 implementation tasks T018-T019 can run together where file boundaries do not conflict.
- US3 implementation tasks T020-T024 can run after shared foundation work.
- Unit test tasks T028-T039 can run in parallel.

## Parallel Example: Unit tests

```text
T028: Test configuration
T029: Parsing tests
T030: Empty/malformed tests
T031: Amount validation tests
T032: Conversion arithmetic tests
T033: Weekend/fallback tests
T034: Missing-currency tests
T035: Reverse conversion tests
T036: Cache tests
T037: Source-failure tests
T038: Cache-origin tests
T039: Complete mandatory behavior coverage
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Stop and validate US1 implementation.
5. Deploy/demo only after the MVP checkpoint passes.

### Incremental Delivery

1. Deliver US1 conversion as the MVP.
2. Add US2 direction swapping.
3. Add US3 cache/offline handling.
4. Finish Polish and run the complete Maven unit test verification in Phase 7.

### Parallel Team Strategy

1. Complete Setup and Foundational together.
2. Developer A implements US1.
3. Developer B implements US2.
4. Developer C implements US3.
5. Merge after each story implementation passes its independent checkpoint.
6. Complete final unit tests in Phase 7.
