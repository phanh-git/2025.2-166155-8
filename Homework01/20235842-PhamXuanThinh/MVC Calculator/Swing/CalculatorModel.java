// MODEL: Performs all calculations. Has no knowledge of the View.

public class CalculatorModel {

    private double calculationValue;

    public void calculate(double firstNumber, double secondNumber, String operator) {
        switch (operator) {
            case "+": calculationValue = firstNumber + secondNumber; break;
            case "-": calculationValue = firstNumber - secondNumber; break;
            case "*": calculationValue = firstNumber * secondNumber; break;
            case "/":
                if (secondNumber == 0) throw new ArithmeticException("Cannot divide by zero");
                calculationValue = firstNumber / secondNumber;
                break;
            default: throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    public double getCalculationValue() {
        return calculationValue;
    }
}
