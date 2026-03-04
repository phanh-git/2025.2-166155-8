import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class CalculatorControllerFX {

    @FXML
    private Label lbl_result;

    @FXML
    private TextField txt_num1;

    @FXML
    private TextField txt_num2;

    @FXML
    void handleAddition(ActionEvent event) {
        try {
        double n1 = Double.parseDouble(txt_num1.getText());
        double n2 = Double.parseDouble(txt_num2.getText());
        
        double sum = n1 + n2;
        
        lbl_result.setText("" + sum);
    } catch (NumberFormatException e) {
        lbl_result.setText("Error!");
    }
    }

}
