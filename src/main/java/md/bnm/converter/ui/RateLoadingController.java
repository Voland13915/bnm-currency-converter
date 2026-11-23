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
        view.hideCacheOffer();
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
            view.hideCacheOffer();
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
            boolean isSourceFailure;
            if (ex instanceof RateLoadException rle) {
                message = rle.getUserMessage();
                md.bnm.converter.service.RateLoadErrorType type = rle.getErrorType();
                isSourceFailure = (type == md.bnm.converter.service.RateLoadErrorType.NETWORK_ERROR
                    || type == md.bnm.converter.service.RateLoadErrorType.TIMEOUT
                    || type == md.bnm.converter.service.RateLoadErrorType.SERVER_ERROR);
            } else {
                message = "Ошибка при загрузке курсов: " + (ex != null && ex.getMessage() != null ? ex.getMessage() : "неизвестная ошибка");
                isSourceFailure = true;
            }

            view.showGlobalError(message);

            // FR-017, US3: If live source fails and cached response exists, offer explicit choice to use cache
            if (isSourceFailure && rateProvider.hasUsableCache()) {
                LocalDate cachedDate = rateProvider.getCachedDate().orElse(null);
                String dateStr = cachedDate != null ? cachedDate.format(DATE_FORMATTER) : "предыдущую дату";
                view.showCacheOffer("Источник недоступен. Доступны сохранённые курсы от " + dateStr + ". Использовать сохранённые курсы?");
            } else {
                view.hideCacheOffer();
            }
        });

        Thread thread = new Thread(task, "rate-loader-" + date);
        thread.setDaemon(true);
        thread.start();
    }

    public void acceptCache() {
        LocalDate selectedDate = view.getDatePicker().getValue();
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }

        try {
            RateSet cachedRateSet = rateProvider.loadFromCache(selectedDate);
            currentRateSet.set(cachedRateSet);
            updateCurrencySelectors(cachedRateSet);
            view.hideCacheOffer();
            view.hideGlobalError();
            view.ratesLoadedProperty().set(true);
        } catch (RateLoadException e) {
            view.hideCacheOffer();
            view.showGlobalError("Не удалось загрузить кэш: " + e.getUserMessage());
            currentRateSet.set(null);
            view.ratesLoadedProperty().set(false);
        }
    }

    public void declineCache() {
        view.hideCacheOffer();
        view.showGlobalError("Использование сохранённых курсов отклонено. Курсы валют недоступны.");
        currentRateSet.set(null);
        view.ratesLoadedProperty().set(false);
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
