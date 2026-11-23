package md.bnm.converter.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import md.bnm.converter.model.Currency;

import java.time.LocalDate;

/**
 * JavaFX View for the Russian-language Currency Converter form.
 * Controls:
 * - Amount input with adjacent validation error label
 * - Source and target currency selectors
 * - Swap button
 * - DatePicker with future date prohibition
 * - Convert button (enabled when amount, currencies, and date are selected and rates are loaded)
 * - Loading indicator
 * - Conversion result and error display areas
 */
public class CurrencyConverterView {

    private final VBox root;

    private final TextField amountField;
    private final Label amountErrorLabel;

    private final ComboBox<Currency> sourceCurrencyCombo;
    private final Button swapButton;
    private final ComboBox<Currency> targetCurrencyCombo;

    private final DatePicker datePicker;

    private final Button convertButton;
    private final ProgressIndicator loadingIndicator;
    private final Label statusLabel;

    private final VBox resultContainer;
    private final Label resultSummaryLabel;
    private final Label resultRateLabel;
    private final Label resultSourceLabel;
    private final Label resultExplanationLabel;

    private final Label globalErrorLabel;

    private final BooleanProperty ratesLoadedProperty;

    public CurrencyConverterView() {
        this.ratesLoadedProperty = new SimpleBooleanProperty(false);

        this.root = new VBox(12);
        this.root.setPadding(new Insets(16));
        this.root.setStyle("-fx-font-family: 'Segoe UI', Tahoma, Arial, sans-serif; -fx-font-size: 13px;");

        // Amount Section
        Label amountLabel = new Label("Сумма:");
        amountLabel.setStyle("-fx-font-weight: bold;");
        this.amountField = new TextField();
        this.amountField.setPromptText("Например: 100 или 100,50");
        this.amountField.setPrefWidth(220);

        this.amountErrorLabel = new Label();
        this.amountErrorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11px;");
        this.amountErrorLabel.setWrapText(true);

        HBox amountRow = new HBox(8, amountField, amountErrorLabel);
        amountRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(amountErrorLabel, Priority.ALWAYS);

        // Currencies Section
        Label currenciesLabel = new Label("Направление конвертации:");
        currenciesLabel.setStyle("-fx-font-weight: bold;");

        this.sourceCurrencyCombo = new ComboBox<>();
        this.sourceCurrencyCombo.setPromptText("Из валюты");
        this.sourceCurrencyCombo.setPrefWidth(180);

        this.swapButton = new Button("⇄");
        this.swapButton.setTooltip(new Tooltip("Поменять валюты местами"));
        this.swapButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 3 10 3 10;");

        this.targetCurrencyCombo = new ComboBox<>();
        this.targetCurrencyCombo.setPromptText("В валюту");
        this.targetCurrencyCombo.setPrefWidth(180);

        StringConverter<Currency> currencyConverter = new StringConverter<>() {
            @Override
            public String toString(Currency c) {
                if (c == null) return "";
                return c.code() + " (" + c.displayName() + ")";
            }

            @Override
            public Currency fromString(String string) {
                return null;
            }
        };
        this.sourceCurrencyCombo.setConverter(currencyConverter);
        this.targetCurrencyCombo.setConverter(currencyConverter);

        HBox currencyRow = new HBox(8, sourceCurrencyCombo, swapButton, targetCurrencyCombo);
        currencyRow.setAlignment(Pos.CENTER_LEFT);

        // Date Section
        Label dateLabel = new Label("Дата курса:");
        dateLabel.setStyle("-fx-font-weight: bold;");

        this.datePicker = new DatePicker(LocalDate.now());
        this.datePicker.setPrefWidth(180);

        // FR-006: Do not allow future dates
        this.datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null && date.isAfter(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #aaaaaa;");
                }
            }
        });
        this.datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isAfter(LocalDate.now())) {
                datePicker.setValue(oldVal != null ? oldVal : LocalDate.now());
            }
        });

        HBox dateRow = new HBox(8, datePicker);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        // Actions & Loading
        this.convertButton = new Button("Конвертировать");
        this.convertButton.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 16 6 16;");

        this.loadingIndicator = new ProgressIndicator();
        this.loadingIndicator.setPrefSize(20, 20);
        this.loadingIndicator.setVisible(false);

        this.statusLabel = new Label();
        this.statusLabel.setStyle("-fx-text-fill: #555555; -fx-font-style: italic;");

        HBox actionRow = new HBox(12, convertButton, loadingIndicator, statusLabel);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        // FR-007: Convert enabled when amount non-empty, both currencies and date selected, and rates loaded
        BooleanBinding canConvert = Bindings.createBooleanBinding(
            () -> amountField.getText() != null && !amountField.getText().trim().isEmpty()
                && sourceCurrencyCombo.getValue() != null
                && targetCurrencyCombo.getValue() != null
                && datePicker.getValue() != null
                && ratesLoadedProperty.get(),
            amountField.textProperty(),
            sourceCurrencyCombo.valueProperty(),
            targetCurrencyCombo.valueProperty(),
            datePicker.valueProperty(),
            ratesLoadedProperty
        );
        this.convertButton.disableProperty().bind(canConvert.not());

        // Result Container
        this.resultContainer = new VBox(6);
        this.resultContainer.setPadding(new Insets(12));
        this.resultContainer.setStyle("-fx-background-color: #e8f5e9; -fx-border-color: #81c784; -fx-border-radius: 4; -fx-background-radius: 4;");
        this.resultContainer.setVisible(false);
        this.resultContainer.setManaged(false);

        this.resultSummaryLabel = new Label();
        this.resultSummaryLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;");

        this.resultRateLabel = new Label();
        this.resultRateLabel.setStyle("-fx-text-fill: #2e7d32;");

        this.resultSourceLabel = new Label();
        this.resultSourceLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11px;");

        this.resultExplanationLabel = new Label();
        this.resultExplanationLabel.setStyle("-fx-text-fill: #e65100; -fx-font-style: italic; -fx-font-size: 12px;");
        this.resultExplanationLabel.setWrapText(true);

        this.resultContainer.getChildren().addAll(
            resultSummaryLabel,
            resultRateLabel,
            resultSourceLabel,
            resultExplanationLabel
        );

        // Global Error Area
        this.globalErrorLabel = new Label();
        this.globalErrorLabel.setStyle("-fx-text-fill: #c62828; -fx-background-color: #ffebee; -fx-padding: 8; -fx-border-color: #ef9a9a; -fx-border-radius: 4; -fx-background-radius: 4;");
        this.globalErrorLabel.setWrapText(true);
        this.globalErrorLabel.setVisible(false);
        this.globalErrorLabel.setManaged(false);

        // Assemble root
        this.root.getChildren().addAll(
            amountLabel,
            amountRow,
            currenciesLabel,
            currencyRow,
            dateLabel,
            dateRow,
            actionRow,
            globalErrorLabel,
            resultContainer
        );
    }

    public VBox getRoot() {
        return root;
    }

    public TextField getAmountField() {
        return amountField;
    }

    public Label getAmountErrorLabel() {
        return amountErrorLabel;
    }

    public ComboBox<Currency> getSourceCurrencyCombo() {
        return sourceCurrencyCombo;
    }

    public Button getSwapButton() {
        return swapButton;
    }

    public ComboBox<Currency> getTargetCurrencyCombo() {
        return targetCurrencyCombo;
    }

    public DatePicker getDatePicker() {
        return datePicker;
    }

    public Button getConvertButton() {
        return convertButton;
    }

    public ProgressIndicator getLoadingIndicator() {
        return loadingIndicator;
    }

    public Label getStatusLabel() {
        return statusLabel;
    }

    public VBox getResultContainer() {
        return resultContainer;
    }

    public Label getResultSummaryLabel() {
        return resultSummaryLabel;
    }

    public Label getResultRateLabel() {
        return resultRateLabel;
    }

    public Label getResultSourceLabel() {
        return resultSourceLabel;
    }

    public Label getResultExplanationLabel() {
        return resultExplanationLabel;
    }

    public Label getGlobalErrorLabel() {
        return globalErrorLabel;
    }

    public BooleanProperty ratesLoadedProperty() {
        return ratesLoadedProperty;
    }

    public void showLoading(String message) {
        loadingIndicator.setVisible(true);
        statusLabel.setText(message != null ? message : "Загрузка курсов...");
        ratesLoadedProperty.set(false);
    }

    public void hideLoading() {
        loadingIndicator.setVisible(false);
        statusLabel.setText("");
    }

    public void showGlobalError(String message) {
        globalErrorLabel.setText(message);
        globalErrorLabel.setVisible(true);
        globalErrorLabel.setManaged(true);
        ratesLoadedProperty.set(false);
    }

    public void hideGlobalError() {
        globalErrorLabel.setText("");
        globalErrorLabel.setVisible(false);
        globalErrorLabel.setManaged(false);
    }

    public void clearResult() {
        resultContainer.setVisible(false);
        resultContainer.setManaged(false);
        resultSummaryLabel.setText("");
        resultRateLabel.setText("");
        resultSourceLabel.setText("");
        resultExplanationLabel.setText("");
    }

    public boolean isResultVisible() {
        return resultContainer.isVisible();
    }

    public void swapCurrencies() {
        Currency source = sourceCurrencyCombo.getValue();
        Currency target = targetCurrencyCombo.getValue();
        sourceCurrencyCombo.setValue(target);
        targetCurrencyCombo.setValue(source);
    }

    public void updateResultAfterSwap(String summary, String rate, String sourceAndDate, String explanation) {
        showResult(summary, rate, sourceAndDate, explanation);
    }

    public void showResult(String summary, String rate, String sourceAndDate, String explanation) {
        resultSummaryLabel.setText(summary);
        resultRateLabel.setText(rate);
        resultSourceLabel.setText(sourceAndDate);

        if (explanation != null && !explanation.isBlank()) {
            resultExplanationLabel.setText("Примечание: " + explanation);
            resultExplanationLabel.setVisible(true);
            resultExplanationLabel.setManaged(true);
        } else {
            resultExplanationLabel.setText("");
            resultExplanationLabel.setVisible(false);
            resultExplanationLabel.setManaged(false);
        }

        resultContainer.setVisible(true);
        resultContainer.setManaged(true);
    }
}
