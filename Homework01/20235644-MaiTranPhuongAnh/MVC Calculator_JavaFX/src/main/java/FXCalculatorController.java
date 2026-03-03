import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class FXCalculatorController {

    @FXML
    private TextField txtA;

    @FXML
    private TextField txtB;

    @FXML
    private Label lblResult;

    private FXCalculatorModel model = new FXCalculatorModel();

    @FXML
    private void handleAdd() {
        try {
            double a = Double.parseDouble(txtA.getText());
            double b = Double.parseDouble(txtB.getText());

            double result = model.add(a, b);

            lblResult.setText("Result: " + result);

        } catch (NumberFormatException e) {
            lblResult.setText("Lỗi: Vui lòng nhập số!");
        }
    }
}