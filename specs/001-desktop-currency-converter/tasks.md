# Tasks: Desktop Currency Converter

**Input**: Design documents from `/specs/001-desktop-currency-converter/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: JUnit 5 unit coverage is required by the constitution and quickstart for parsing, conversion, validation, and fallback behavior.

**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize the Maven Java 21 and JavaFX 21 application.

- [ ] T001 Create the Maven project structure and `pom.xml` with Java 21, JavaFX 21, the JavaFX Maven plugin, and JUnit 5 at repository root
- [ ] T002 [P] Create package directories for `src/main/java/md/bnm/converter/{model,source,cache,service,ui}` and `src/test/java/md/bnm/converter/{model,source,cache,service,ui}` in the repository
- [ ] T003 [P] Add the Maven test and JavaFX run configuration in `pom.xml`
- [ ] T004 [P] Add empty and malformed XML fixtures in `src/test/resources/bnm-empty.xml` and `src/test/resources/bnm-malformed.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core types and infrastructure that every user story requires.

**CRITICAL**: No user-story implementation can begin until this phase is complete.

- [ ] T005 Create immutable `Currency`, `PublishedRate`, `RateSet`, `ConversionResult`, and `CachedResponse` records in `src/main/java/md/bnm/converter/model/` with fields and invariants from `data-model.md`
- [ ] T006 [P] Implement secure JAXP XML parsing and root-date/rate validation in `src/main/java/md/bnm/converter/source/NbmXmlParser.java`
- [ ] T007 [P] Implement NBM HTTP retrieval with JDK `HttpClient`, a 10-second timeout, status handling, and live-result errors in `src/main/java/md/bnm/converter/source/NbmRateSource.java`
- [ ] T008 [P] Implement persistent raw-XML cache read/write and corrupted-cache handling in `src/main/java/md/bnm/converter/cache/ResponseCache.java`
- [ ] T009 Implement shared source/result exceptions and error-state types in `src/main/java/md/bnm/converter/service/RateLoadException.java`
- [ ] T010 Implement a JavaFX-independent rate provider that tracks loaded dates, searches D-1 through D-10 for empty responses, and applies weekend context in `src/main/java/md/bnm/converter/service/RateProvider.java`
- [ ] T011 Add reusable JUnit 5 test configuration and baseline test base classes under `src/test/java/md/bnm/converter/`

**Checkpoint**: Foundation ready; exact domain types, source parsing, cache, and rate loading are available.

---

## Phase 3: User Story 1 - Convert an amount (Priority: P1) MVP

**Goal**: Provide exact through-MDL conversion with Russian validation and traceable result details.

**Independent Test**: Select available currencies and a dated rate set, enter valid and invalid amounts, and verify conversion, validation, weekend, fallback, and missing-currency behavior.

### Tests for User Story 1

- [ ] T012 [P] [US1] Test parsing of real weekday and weekend fixtures plus required `Published Rate` fields in `src/test/java/md/bnm/converter/source/NbmXmlParserTest.java` (cite `data-model.md` `Published Rate.Nominal`, `Published Rate.Value`, and `Published Rate.Currency code`)
- [ ] T013 [P] [US1] Test empty and malformed XML distinction in `src/test/java/md/bnm/converter/source/NbmXmlParserTest.java`
- [ ] T014 [P] [US1] Test period/comma amount parsing and field constraints in `src/test/java/md/bnm/converter/service/AmountValidatorTest.java` (cite `data-model.md` `Amount Validation`, including positive and at most 1,000,000,000 with at most two fractional digits)
- [ ] T015 [P] [US1] Test same-currency and cross-currency `BigDecimal` conversion, four-decimal display, and reverse-direction rounding in `src/test/java/md/bnm/converter/service/CurrencyConverterTest.java` (cite `data-model.md` `Published Rate.Per-unit rate`, `Conversion Result.Converted amount`, and `Conversion Result.Cross-rate`)
- [ ] T016 [P] [US1] Test weekend context and D-1 through D-10 empty-response fallback in `src/test/java/md/bnm/converter/service/RateProviderTest.java` (cite `data-model.md` `Rate Set.requestedDate`, `Rate Set.applicableDate`, and `Rate Set.explanation`)
- [ ] T017 [P] [US1] Test unavailable-currency blocking in `src/test/java/md/bnm/converter/service/CurrencyConverterTest.java`

### Implementation for User Story 1

- [ ] T018 [P] [US1] Implement amount validation for `data-model.md` `Amount Validation`: period or comma separator, at most two fractional digits, `> 0`, and maximum 1,000,000,000 in `src/main/java/md/bnm/converter/service/AmountValidator.java`
- [ ] T019 [US1] Implement through-MDL conversion and unavailable-currency checks in `src/main/java/md/bnm/converter/service/CurrencyConverter.java`, using `data-model.md` `Published Rate.Per-unit rate` and `Conversion Result.Converted amount`
- [ ] T020 [US1] Implement conversion result formatting for two-decimal amounts, cross-rates to no more than four decimals, requested/applicable dates, NBM source, and fallback text in `src/main/java/md/bnm/converter/service/ConversionResultFormatter.java`
- [ ] T021 [US1] Build the Russian JavaFX form for amount, currency selectors, date selection, result/error areas, and future-date prohibition in `src/main/java/md/bnm/converter/ui/CurrencyConverterView.java`
- [ ] T022 [US1] Implement JavaFX `Task`-based startup and date-change loading plus session-date deduplication in `src/main/java/md/bnm/converter/ui/RateLoadingController.java`
- [ ] T023 [US1] Wire Convert to validation, loaded-rate checks, conversion, and Russian field-level errors in `src/main/java/md/bnm/converter/ui/CurrencyConverterController.java`
- [ ] T024 [US1] Add Russian weekend and earlier-date fallback explanations to `src/main/java/md/bnm/converter/ui/CurrencyConverterController.java` using `data-model.md` `Rate Set.explanation`
- [ ] T025 [US1] Create the JavaFX application entry point in `src/main/java/md/bnm/converter/ui/ConverterApplication.java`

**Checkpoint**: User Story 1 works independently: valid conversion and all specified amount/rate-date/missing-currency outcomes are testable.

---

## Phase 4: User Story 2 - Swap currency direction (Priority: P2)

**Goal**: Let the user reverse source and target immediately and preserve traceable conversion behavior.

**Independent Test**: Select two currencies, activate Swap, and verify the selectors change and reverse conversion is correct.

### Tests for User Story 2

- [ ] T026 [P] [US2] Test selector swapping and unchanged amount/rate selections in `src/test/java/md/bnm/converter/ui/CurrencySwapTest.java`
- [ ] T027 [P] [US2] Test reverse conversion recovery within two-decimal display rounding in `src/test/java/md/bnm/converter/service/CurrencyConverterTest.java`

### Implementation for User Story 2

- [ ] T028 [US2] Implement Swap behavior in `src/main/java/md/bnm/converter/ui/CurrencyConverterController.java`
- [ ] T029 [US2] Update result rendering after Swap in `src/main/java/md/bnm/converter/ui/CurrencyConverterView.java`

**Checkpoint**: User Stories 1 and 2 work independently; swapping reverses selections and recalculates through MDL.

---

## Phase 5: User Story 3 - Continue when the source is unavailable (Priority: P3)

**Goal**: Offer persistent cached rates after live-source failures without producing success-shaped or untraceable results.

**Independent Test**: Save a successful response, restart, make the source unavailable, and verify explicit cache acceptance and cache-date/origin traceability.

### Tests for User Story 3

- [ ] T030 [P] [US3] Test cache writes, restart reloading, and ignored corrupted cache data in `src/test/java/md/bnm/converter/cache/ResponseCacheTest.java` (cite `data-model.md` `Cached Response`)
- [ ] T031 [P] [US3] Test timeout, network, HTTP, empty-rate, malformed-response, and no-cache messages in `src/test/java/md/bnm/converter/service/RateProviderTest.java`
- [ ] T032 [P] [US3] Test that cache origin is applied only after explicit acceptance in `src/test/java/md/bnm/converter/service/RateProviderTest.java` (cite `data-model.md` `Rate Set.origin`)

### Implementation for User Story 3

- [ ] T033 [US3] Persist the last successfully parsed raw NBM XML response using `data-model.md` `Cached Response` in `src/main/java/md/bnm/converter/cache/ResponseCache.java`
- [ ] T034 [US3] Reparse cached XML, reject corrupted cache data, and mark cache origin only after acceptance in `src/main/java/md/bnm/converter/service/RateProvider.java`
- [ ] T035 [US3] Add Russian source-failure and explicit cached-rate offer states to `src/main/java/md/bnm/converter/ui/RateLoadingController.java`
- [ ] T036 [US3] Wire cached-rate acceptance and rejection into `src/main/java/md/bnm/converter/ui/CurrencyConverterController.java`
- [ ] T037 [US3] Add cache date and local-cache origin to successful result rendering in `src/main/java/md/bnm/converter/ui/CurrencyConverterView.java`

**Checkpoint**: All user stories are independently functional, and live/cache failures remain usable and traceable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification, documentation, and robustness across stories.

- [ ] T038 [P] Add or complete JUnit 5 tests in `src/test/java/md/bnm/converter/` for all mandatory behaviors, preserving the real fixtures under `src/test/resources/`
- [ ] T039 [P] Add Russian error strings and consistent result labels across `src/main/java/md/bnm/converter/ui/`
- [ ] T040 Verify Maven dependencies remain limited to JavaFX and JUnit 5 in `pom.xml`
- [ ] T041 Run `mvn test` and resolve failures in the repository
- [ ] T042 Run `mvn javafx:run` and validate startup, conversion, Swap, fallback, and cache workflows against `specs/001-desktop-currency-converter/quickstart.md`
- [ ] T043 [P] Document build, test, and run commands in `README.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; starts immediately.
- **Foundational (Phase 2)**: Depends on Setup completion and blocks every user story.
- **User Stories (Phase 3+)**: Depend on Foundational completion.
  - US1, US2, and US3 can proceed independently after Foundation when staffed separately.
  - Sequential P1 -> P2 -> P3 execution preserves priority order.
- **Polish (Phase 6)**: Depends on all selected user stories being complete.

### User Story Dependencies

- **US1 (P1)**: No dependency on US2 or US3.
- **US2 (P2)**: Uses the US1 conversion and UI state but remains independently testable.
- **US3 (P3)**: Uses the shared rate source/cache foundation and US1 result state but remains independently testable.

### Within Each User Story

- Tests are written first and must fail before implementation.
- Data models precede services.
- Services precede UI integration.
- Core implementation precedes fallback/cache result integration.
- Complete and validate one story checkpoint before its priority successor if not running in parallel.

---

## Parallel Opportunities

- T002-T004 can run after T001.
- T006-T008 can run concurrently after model types in T005.
- US1 tests T012-T017 can run in parallel.
- US1 model-independent UI and service tasks can run in separate workstreams after T018-T020.
- US2 test tasks T026-T027 can run together.
- US3 cache tests T030-T032 can run together.
- Polish documentation and test work T038-T040/T043 can run concurrently.

## Parallel Example: User Story 1

```text
T012: Parsing tests
T013: Empty/malformed tests
T014: Amount validation tests
T015: Conversion arithmetic tests
T016: Weekend/fallback tests
T017: Missing-currency tests
```

Implementation can similarly split T018 validation, T019 conversion, T020 formatting, and T021-T025 UI work where file boundaries do not conflict.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Stop and validate US1 with `mvn test` and the manual US1 quickstart scenarios.
5. Deploy/demo only after the MVP checkpoint passes.

### Incremental Delivery

1. Deliver US1 conversion as the MVP.
2. Add US2 direction swapping and validate it independently.
3. Add US3 cache/offline handling and validate restart behavior.
4. Finish Polish and run the complete Maven validation commands.

### Parallel Team Strategy

1. Complete Setup and Foundational together.
2. Developer A implements US1.
3. Developer B implements US2 and reverse-direction tests.
4. Developer C implements US3 cache and failure tests.
5. Merge only after each story passes its independent checkpoint.
