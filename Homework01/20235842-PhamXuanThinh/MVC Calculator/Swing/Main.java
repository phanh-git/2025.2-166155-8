// ENTRY POINT: Boots up the MVC triad.

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CalculatorView       theView       = new CalculatorView();
            CalculatorModel      theModel      = new CalculatorModel();
            CalculatorController theController = new CalculatorController(theView, theModel);
            theView.setVisible(true);
        });
    }
}
