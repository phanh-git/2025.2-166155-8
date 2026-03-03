// ENTRY POINT: Loads the FXML (View) and launches the JavaFX stage.

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // FXMLLoader reads calculator.fxml and instantiates CalculatorController
        Parent root = FXMLLoader.load(getClass().getResource("calculator.fxml"));

        primaryStage.setTitle("MVC Calculator - JavaFX");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
