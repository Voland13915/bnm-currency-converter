package md.bnm.converter.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import md.bnm.converter.service.RateProvider;

/**
 * Main JavaFX application entry point for the BNM Currency Converter.
 */
public class ConverterApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        RateProvider rateProvider = new RateProvider();
        CurrencyConverterView view = new CurrencyConverterView();
        CurrencyConverterController controller = new CurrencyConverterController(view, rateProvider);
        controller.initialize();

        Scene scene = new Scene(view.getRoot(), 540, 440);
        primaryStage.setTitle("Конвертер валют НБМ");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(480);
        primaryStage.setMinHeight(380);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
