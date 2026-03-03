// CONTROLLER (JavaFX): FXML injects UI nodes via @FXML.
// This class acts as the MVC Controller; the .fxml file is the View.

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class CalculatorController {

    // ── Injected by FXMLLoader ──────────────────────────────────────
    @FXML private TextField displayField;

    // ── Model ───────────────────────────────────────────────────────
    private final CalculatorModel theModel = new CalculatorModel();

    // ── Controller state ────────────────────────────────────────────
    private double  firstOperand = 0;
    private String  pendingOp    = "";
    private boolean freshOperand = false;

    // ── FXML action handlers ────────────────────────────────────────

    @FXML
    private void handleNumber(ActionEvent e) {
        String digit = ((Button) e.getSource()).getText();
        if (freshOperand) {
            displayField.setText("");
            freshOperand = false;
        }
        displayField.setText(displayField.getText() + digit);
    }

    @FXML
    private void handleDot(ActionEvent e) {
        if (freshOperand) {
            displayField.setText("0.");
            freshOperand = false;
            return;
        }
        if (!displayField.getText().contains(".")) {
            if (displayField.getText().isEmpty()) displayField.setText("0");
            displayField.setText(displayField.getText() + ".");
        }
    }

    @FXML private void handleAdd(ActionEvent e) { handleOperator("+"); }
    @FXML private void handleSub(ActionEvent e) { handleOperator("-"); }
    @FXML private void handleMul(ActionEvent e) { handleOperator("*"); }
    @FXML private void handleDiv(ActionEvent e) { handleOperator("/"); }

    @FXML
    private void handleEquals(ActionEvent e) {
        if (pendingOp.isEmpty()) return;
        try {
            double secondOperand = Double.parseDouble(displayField.getText());
            theModel.calculate(firstOperand, secondOperand, pendingOp);
            double result = theModel.getCalculationValue();

            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                displayField.setText(String.valueOf((long) result));
            } else {
                displayField.setText(String.valueOf(result));
            }
            pendingOp    = "";
            freshOperand = true;
        } catch (NumberFormatException ex) {
            showError("Invalid number entered.");
        } catch (ArithmeticException ex) {
            showError(ex.getMessage());
            handleClear(e);
        }
    }

    @FXML
    private void handleClear(ActionEvent e) {
        displayField.setText("");
        firstOperand = 0;
        pendingOp    = "";
        freshOperand = false;
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private void handleOperator(String op) {
        try {
            firstOperand = Double.parseDouble(displayField.getText());
            pendingOp    = op;
            freshOperand = true;
        } catch (NumberFormatException ex) {
            showError("Enter a valid number first.");
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}