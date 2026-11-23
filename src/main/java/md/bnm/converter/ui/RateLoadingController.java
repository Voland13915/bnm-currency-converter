package md.bnm.converter.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import md.bnm.converter.model.Currency;
import md.bnm.converter.model.PublishedRate;
import md.bnm.converter.model.RateSet;
import md.bnm.converter.service.RateLoadException;
import md.bnm.converter.service.RateProvider;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Controller for asynchronous exchange rate loading using JavaFX Task.
 * Handles rate retrieval on application startup and whenever the selected date changes.
 * Tracks session-loaded dates via RateProvider to avoid redundant network requests.
 * Updates currency selection dropdowns on success and reports user-facing errors on failure.
 */
public class RateLoadingController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final CurrencyConverterView view;
    private final RateProvider rateProvider;
    private final ObjectProperty<RateSet> currentRateSet;

    public RateLoadingController(CurrencyConverterView view, RateProvider rateProvider) {
        this.view = Objects.requireNonNull(view, "view must not be null");
        this.rateProvider = Objects.requireNonNull(rateProvider, "rateProvider must not be null");
        this.currentRateSet = new SimpleObjectProperty<>(null);
    }

    public void initialize() {
        view.getDatePicker().valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                loadRates(newDate);
            }
        });

        LocalDate initialDate = view.getDatePicker().getValue();
        if (initialDate == null) {
            initialDate = LocalDate.now();
            view.getDatePicker().setValue(initialDate);
        }
        loadRates(initialDate);
    }

    public void loadRates(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            view.showGlobalError("Выбор будущей даты не допускается.");
            return;
        }

        view.clearResult();
        view.hideGlobalError();
        view.showLoading("Загрузка официальных курсов валют на " + date.format(DATE_FORMATTER) + "...");

        Task<RateSet> task = new Task<>() {
            @Override
            protected RateSet call() throws Exception {
                return rateProvider.getRates(date);
            }
        };

        task.setOnSucceeded(event -> {
            RateSet rateSet = task.getValue();
            view.hideLoading();
            view.hideGlobalError();
            currentRateSet.set(rateSet);
            updateCurrencySelectors(rateSet);
            view.ratesLoadedProperty().set(true);
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            view.hideLoading();
            currentRateSet.set(null);
            view.ratesLoadedProperty().set(false);

            String message;
            if (ex instanceof RateLoadException rle) {
                message = rle.getUserMessage();
            } else {
                message = "Ошибка при загрузке курсов: " + (ex != null && ex.getMessage() != null ? ex.getMessage() : "неизвестная ошибка");
            }
            view.showGlobalError(message);
        });

        Thread thread = new Thread(task, "rate-loader-" + date);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateCurrencySelectors(RateSet rateSet) {
        Currency oldSource = view.getSourceCurrencyCombo().getValue();
        Currency oldTarget = view.getTargetCurrencyCombo().getValue();

        List<Currency> currencies = rateSet.rates().values().stream()
            .map(PublishedRate::currency)
            .distinct()
            .sorted(Comparator.comparing(Currency::code))
            .toList();

        view.getSourceCurrencyCombo().getItems().setAll(currencies);
        view.getTargetCurrencyCombo().getItems().setAll(currencies);

        // Restore or set defaults
        if (oldSource != null && currencies.contains(oldSource)) {
            view.getSourceCurrencyCombo().setValue(oldSource);
        } else {
            Currency defaultSource = currencies.stream()
                .filter(c -> "EUR".equalsIgnoreCase(c.code()))
                .findFirst()
                .orElse(currencies.isEmpty() ? null : currencies.get(0));
            view.getSourceCurrencyCombo().setValue(defaultSource);
        }

        if (oldTarget != null && currencies.contains(oldTarget)) {
            view.getTargetCurrencyCombo().setValue(oldTarget);
        } else {
            Currency defaultTarget = currencies.stream()
                .filter(c -> "MDL".equalsIgnoreCase(c.code()))
                .findFirst()
                .orElse(currencies.isEmpty() ? null : currencies.get(0));
            view.getTargetCurrencyCombo().setValue(defaultTarget);
        }
    }

    public ObjectProperty<RateSet> currentRateSetProperty() {
        return currentRateSet;
    }

    public RateSet getCurrentRateSet() {
        return currentRateSet.get();
    }
}
