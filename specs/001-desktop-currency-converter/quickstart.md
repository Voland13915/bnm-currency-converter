# Quickstart and Validation

## Prerequisites

- Java 21 JDK
- Maven
- A desktop environment supported by JavaFX 21

## Build and Automated Validation

From the repository root:

```powershell
mvn test
```

Expected outcome: JUnit 5 tests pass, covering the real weekday/weekend XML fixtures, empty and malformed responses, conversion arithmetic, and amount validation.

## Run the Desktop Application

```powershell
mvn javafx:run
```

Expected outcome: a Russian-language window opens with today's date selected and rates loading without freezing the interface.

## Manual Scenarios

1. Select two available currencies, enter a valid amount, and convert. Confirm the result shows the converted amount, rate, applicable date, and NBM source.
2. Convert the same amount in the reverse direction using Swap; confirm the result returns to the original amount within display rounding.
3. Try empty, non-numeric, zero, negative, over-two-decimal, and above-maximum amounts. Confirm each is blocked with an adjacent error.
4. Select a Saturday or Sunday. Confirm the result explains that the previous Friday's rate applies and does not claim NBM set a weekend rate.
5. Exercise a date with no rates and verify the D-1 through D-10 search and no-rates outcome.
6. After a successful load, make the source unavailable. Confirm the app offers cached rates, and only after acceptance displays a result marked with the cache date and origin.
7. Select a future date and confirm it cannot be selected.

See [data-model.md](./data-model.md) for rate and result semantics and [contracts/desktop-ui.md](./contracts/desktop-ui.md) for UI behavior.
