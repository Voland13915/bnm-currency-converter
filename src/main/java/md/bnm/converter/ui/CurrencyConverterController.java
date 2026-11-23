package md.bnm.converter.ui;

import md.bnm.converter.model.ConversionResult;
import md.bnm.converter.model.Currency;
import md.bnm.converter.model.RateSet;
import md.bnm.converter.service.AmountValidator;
import md.bnm.converter.service.ConversionResultFormatter;
import md.bnm.converter.service.CurrencyConverter;
import md.bnm.converter.service.RateLoadException;
import md.bnm.converter.service.RateProvider;

import java.util.Objects;

/**
 * Main UI Controller for currency conversion.
 * Wires Convert button to validation, loaded-rate checks, conversion, and Russian field-level errors.
 * Renders conversion results including rate details, applicable date, NBM source attribution,
 * and weekend/earlier-date fallback explanations.
 */
public class CurrencyConverterController {

    private final CurrencyConverterView view;
    private final RateProvider rateProvider;
    private final RateLoadingController rateLoadingController;
    private final CurrencyConverter currencyConverter;

    public CurrencyConverterController(CurrencyConverterView view, RateProvider rateProvider) {
        this.view = Objects.requireNonNull(view, "view must not be null");
        this.rateProvider = Objects.requireNonNull(rateProvider, "rateProvider must not be null");
        this.rateLoadingController = new RateLoadingController(view, rateProvider);
        this.currencyConverter = new CurrencyConverter();
    }

    public CurrencyConverterController(CurrencyConverterView view, RateProvider rateProvider, CurrencyConverter currencyConverter) {
        this.view = Objects.requireNonNull(view, "view must not be null");
        this.rateProvider = Objects.requireNonNull(rateProvider, "rateProvider must not be null");
        this.rateLoadingController = new RateLoadingController(view, rateProvider);
        this.currencyConverter = Objects.requireNonNull(currencyConverter, "currencyConverter must not be null");
    }

    public void initialize() {
        rateLoadingController.initialize();

        // Convert action handlers
        view.getConvertButton().setOnAction(e -> handleConvert());
        view.getAmountField().setOnAction(e -> {
            if (!view.getConvertButton().isDisabled()) {
                handleConvert();
            }
        });

        // Swap action handler (FR-012, US2)
        view.getSwapButton().setOnAction(e -> handleSwap());

        // Cache acceptance and rejection handlers (FR-017, US3)
        view.getAcceptCacheButton().setOnAction(e -> handleAcceptCache());
        view.getDeclineCacheButton().setOnAction(e -> handleDeclineCache());

        // Clear amount error when user modifies the input
        view.getAmountField().textProperty().addListener((obs, oldVal, newVal) -> {
            view.getAmountErrorLabel().setText("");
        });
    }

    public void handleSwap() {
        Currency source = view.getSourceCurrencyCombo().getValue();
        Currency target = view.getTargetCurrencyCombo().getValue();

        if (source == null && target == null) {
            return;
        }

        view.swapCurrencies();

        // If a result is currently displayed, immediately recalculate and update result rendering
        if (view.isResultVisible()) {
            handleConvert();
        }
    }

    public void handleAcceptCache() {
        rateLoadingController.acceptCache();
    }

    public void handleDeclineCache() {
        rateLoadingController.declineCache();
        view.clearResult();
    }

    public void handleConvert() {
        view.getAmountErrorLabel().setText("");
        view.clearResult();

        String rawAmount = view.getAmountField().getText();

        // FR-007, FR-009: Amount validation checked when Convert is pressed
        AmountValidator.ValidationResult validationResult = AmountValidator.validate(rawAmount);
        if (!validationResult.isValid()) {
            view.getAmountErrorLabel().setText(validationResult.errorMessage());
            return;
        }

        Currency source = view.getSourceCurrencyCombo().getValue();
        Currency target = view.getTargetCurrencyCombo().getValue();

        if (source == null || target == null) {
            view.showGlobalError("Пожалуйста, выберите обе валюты.");
            return;
        }

        RateSet rateSet = rateLoadingController.getCurrentRateSet();
        if (rateSet == null) {
            if (rateProvider.hasUsableCache() && !view.isCacheOfferVisible()) {
                java.time.LocalDate cachedDate = rateProvider.getCachedDate().orElse(null);
                String dateStr = cachedDate != null ? cachedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "предыдущую дату";
                view.showCacheOffer("Источник недоступен. Доступны сохранённые курсы от " + dateStr + ". Использовать сохранённые курсы?");
            }
            view.showGlobalError("Официальные курсы валют на выбранную дату ещё не загружены.");
            return;
        }

        try {
            ConversionResult result = currencyConverter.convert(
                validationResult.amount(),
                source,
                target,
                rateSet
            );

            displayResult(result);
        } catch (RateLoadException e) {
            view.showGlobalError(e.getUserMessage());
        }
    }

    private void displayResult(ConversionResult result) {
        String summary = ConversionResultFormatter.formatConversionSummary(result);
        String rate = "Курс: " + ConversionResultFormatter.formatRateDetails(result);
        String sourceAndDate = ConversionResultFormatter.formatSourceAndDate(result);
        String explanation = result.explanation();

        view.updateResultAfterSwap(summary, rate, sourceAndDate, explanation);
    }

    public RateLoadingController getRateLoadingController() {
        return rateLoadingController;
    }
}
