<!--
Sync Impact Report
Version change: unversioned template → 1.0.0 (initial adoption)
Modified principles: unfilled principle placeholders → I–VIII, as listed below
Added sections: Platform & Dependencies; Specification & Delivery
Removed sections: none
Follow-up TODOs: confirm the original ratification date
-->

# BNM Currency Converter Constitution

## Core Principles

### I. Exact Monetary Arithmetic
All monetary amounts and exchange rates MUST use `BigDecimal`; floating-point
types (`float` and `double`) MUST NOT be used for monetary calculations. Rates
MUST retain the precision supplied by the source. Converted results MUST be
displayed rounded to two decimal places using `HALF_UP`.

### II. Resilient User Experience
Network, parsing, and file errors MUST be handled and shown to the user as short,
plain-language messages. Error handling MUST leave the application window usable;
an operational error MUST NOT crash the application.

### III. Traceable Results
Every conversion result MUST identify its exchange-rate date and NBM as its
source. If the application uses an earlier published date because no rate was
published for the requested date, it MUST state that fallback next to the result.
If it uses cached data because the network failed, it MUST state that fallback
next to the result.

### IV. Offline-First Data
The application MUST store the last successful NBM source response locally so
that it survives an application restart. When the network is unavailable, the
application MUST offer the cached data and identify its date.

### V. UI-Independent Domain Logic
Fetching, parsing, caching, conversion, and input validation MUST reside in plain
Java classes without JavaFX dependencies. This separation MUST allow those
behaviors to be tested independently of the UI.

### VI. Behavior Is Covered by Tests
Unit tests MUST cover source parsing, conversion in both directions, empty and
malformed source responses, and input validation. The tests MUST run with one
documented command.

### VII. Minimal, Approved Technology
The application MUST use Java 21, JavaFX, Maven, and JUnit 5. XML processing and
HTTP communication MUST use the JDK standard library. The project MUST NOT add
a dependency unless its reason is stated in the implementation plan.

### VIII. Specification Is Authoritative
Implementation MUST conform to `spec.md` and `plan.md`. Any necessary deviation
MUST be documented rather than silently introduced.

## Platform & Dependencies

Java 21 is the application runtime and language baseline. JavaFX is reserved for
the desktop UI; core behavior remains in Java classes independent of JavaFX.
Maven is the build tool and JUnit 5 is the test framework. XML and HTTP use JDK
standard-library APIs; additional dependencies require a stated plan rationale.

## Specification & Delivery

`spec.md` defines required product behavior and `plan.md` records the approved
implementation approach. Tests for the mandated behaviors MUST be runnable
through one documented command. Changes MUST preserve these requirements or
document an approved deviation in the relevant project artifacts.

## Governance

This constitution governs implementation and review of the BNM Currency
Converter. Amendments MUST be made explicitly in this file and MUST update the
version and last-amended date. Version changes follow semantic versioning:
MAJOR for incompatible changes to principles, MINOR for added or materially
expanded principles or sections, and PATCH for clarifications and other
non-semantic refinements.

Every implementation plan and code review MUST check relevant work against this
constitution, `spec.md`, and `plan.md`. Reviewers MUST verify the monetary
precision and rounding rules, error and fallback messaging, offline cache
behavior, UI-independent logic, required tests, and dependency constraints
where applicable. Any deviation MUST be documented and reviewed rather than
silently accepted.

**Version**: 1.0.0 | **Ratified**: TODO(RATIFICATION_DATE): confirm original adoption date | **Last Amended**: 2026-11-23
