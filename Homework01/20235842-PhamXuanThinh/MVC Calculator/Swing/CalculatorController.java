// CONTROLLER: Wires the View and Model together. Handles user interactions.

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CalculatorController {

    private final CalculatorView  theView;
    private final CalculatorModel theModel;

    // State kept in controller only - view just shows text
    private double  firstOperand  = 0;
    private String  pendingOp     = "";
    private boolean freshOperand  = false; // true = next digit starts a new number

    public CalculatorController(CalculatorView view, CalculatorModel model) {
        this.theView  = view;
        this.theModel = model;
        attachListeners();
    }

    private void attachListeners() {
        // Digit buttons 0-9
        for (int i = 0; i <= 9; i++) {
            final String digit = String.valueOf(i);
            theView.addNumberListener(i, e -> handleDigit(digit));
        }
        theView.addDotListener(e -> handleDot());
        theView.addAddListener(e -> handleOperator("+"));
        theView.addSubListener(e -> handleOperator("-"));
        theView.addMulListener(e -> handleOperator("*"));
        theView.addDivListener(e -> handleOperator("/"));
        theView.addEqualsListener(e -> handleEquals());
        theView.addClearListener(e -> handleClear());
    }

    private void handleDigit(String digit) {
        if (freshOperand) {
            theView.clearDisplay();
            freshOperand = false;
        }
        theView.appendToDisplay(digit);
    }

    private void handleDot() {
        if (freshOperand) {
            theView.setDisplay("0.");
            freshOperand = false;
            return;
        }
        if (!theView.getDisplay().contains(".")) {
            if (theView.getDisplay().isEmpty()) theView.setDisplay("0");
            theView.appendToDisplay(".");
        }
    }

    private void handleOperator(String op) {
        try {
            firstOperand = Double.parseDouble(theView.getDisplay());
            pendingOp    = op;
            freshOperand = true;
        } catch (NumberFormatException ex) {
            theView.displayErrorMessage("Enter a valid number first.");
        }
    }

    private void handleEquals() {
        if (pendingOp.isEmpty()) return;
        try {
            double secondOperand = Double.parseDouble(theView.getDisplay());
            theModel.calculate(firstOperand, secondOperand, pendingOp);
            double result = theModel.getCalculationValue();

            // Show without trailing .0 when result is a whole number
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                theView.setDisplay(String.valueOf((long) result));
            } else {
                theView.setDisplay(String.valueOf(result));
            }
            pendingOp    = "";
            freshOperand = true;
        } catch (NumberFormatException ex) {
            theView.displayErrorMessage("Invalid number entered.");
        } catch (ArithmeticException ex) {
            theView.displayErrorMessage(ex.getMessage());
            handleClear();
        }
    }

    private void handleClear() {
        theView.clearDisplay();
        firstOperand = 0;
        pendingOp    = "";
        freshOperand = false;
    }
}
