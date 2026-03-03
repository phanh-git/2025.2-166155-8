import javax.swing.*;
import java.awt.*;

public class CalculatorView extends JFrame {
    JTextField txtA = new JTextField(5);
    JTextField txtB = new JTextField(5);
    JButton btnAdd = new JButton("Add");
    JLabel lblResult = new JLabel("Result: ");

    public CalculatorView() {
        setTitle("Calculator MVC - Swing");
        setSize(300, 150);
        setLayout(new FlowLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        add(new JLabel("A:"));
        add(txtA);
        add(new JLabel("B:"));
        add(txtB);
        add(btnAdd);
        add(lblResult);
    }

    public double getA() {
        return Double.parseDouble(txtA.getText());
    }

    public double getB() {
        return Double.parseDouble(txtB.getText());
    }

    public void setResult(double result) {
        lblResult.setText("Result: " + result);
    }
}