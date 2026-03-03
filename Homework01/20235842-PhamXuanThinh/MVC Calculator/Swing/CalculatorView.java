// VIEW: Displays the UI. No business logic here.

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;

public class CalculatorView extends JFrame {

    private JTextField displayField = new JTextField();
    private JButton[] numberButtons = new JButton[10];
    private JButton btnAdd    = new JButton("+");
    private JButton btnSub    = new JButton("-");
    private JButton btnMul    = new JButton("*");
    private JButton btnDiv    = new JButton("/");
    private JButton btnEquals = new JButton("=");
    private JButton btnClear  = new JButton("C");
    private JButton btnDot    = new JButton(".");

    public CalculatorView() {
        setTitle("MVC Calculator - Swing");
        setSize(320, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout(5, 5));

        // --- Display ---
        displayField.setFont(new Font("Monospaced", Font.BOLD, 24));
        displayField.setHorizontalAlignment(JTextField.RIGHT);
        displayField.setEditable(false);
        displayField.setBackground(Color.WHITE);
        displayField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 5, 10),
                BorderFactory.createLineBorder(Color.GRAY, 1)));
        displayField.setPreferredSize(new Dimension(300, 60));
        add(displayField, BorderLayout.NORTH);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new GridLayout(5, 4, 6, 6));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        buttonPanel.setBackground(new Color(245, 245, 245));

        // Row 1
        buttonPanel.add(styleButton(btnClear, new Color(231, 76, 60), Color.WHITE));
        buttonPanel.add(new JLabel()); // spacer
        buttonPanel.add(new JLabel()); // spacer
        buttonPanel.add(styleButton(btnDiv, new Color(52, 152, 219), Color.WHITE));

        // Row 2
        for (int i = 7; i <= 9; i++) {
            numberButtons[i] = new JButton(String.valueOf(i));
            buttonPanel.add(styleButton(numberButtons[i], Color.WHITE, Color.DARK_GRAY));
        }
        buttonPanel.add(styleButton(btnMul, new Color(52, 152, 219), Color.WHITE));

        // Row 3
        for (int i = 4; i <= 6; i++) {
            numberButtons[i] = new JButton(String.valueOf(i));
            buttonPanel.add(styleButton(numberButtons[i], Color.WHITE, Color.DARK_GRAY));
        }
        buttonPanel.add(styleButton(btnSub, new Color(52, 152, 219), Color.WHITE));

        // Row 4
        for (int i = 1; i <= 3; i++) {
            numberButtons[i] = new JButton(String.valueOf(i));
            buttonPanel.add(styleButton(numberButtons[i], Color.WHITE, Color.DARK_GRAY));
        }
        buttonPanel.add(styleButton(btnAdd, new Color(52, 152, 219), Color.WHITE));

        // Row 5
        numberButtons[0] = new JButton("0");
        buttonPanel.add(styleButton(numberButtons[0], Color.WHITE, Color.DARK_GRAY));
        buttonPanel.add(styleButton(btnDot, Color.WHITE, Color.DARK_GRAY));
        buttonPanel.add(new JLabel()); // spacer
        buttonPanel.add(styleButton(btnEquals, new Color(46, 204, 113), Color.WHITE));

        add(buttonPanel, BorderLayout.CENTER);
        setLocationRelativeTo(null);
    }

    private JButton styleButton(JButton btn, Color bg, Color fg) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 18));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        return btn;
    }

    // --- Accessors for Controller ---
    public String getDisplay()                          { return displayField.getText(); }
    public void   setDisplay(String text)               { displayField.setText(text); }
    public void   appendToDisplay(String text)          { displayField.setText(displayField.getText() + text); }
    public void   clearDisplay()                        { displayField.setText(""); }
    public void   displayErrorMessage(String msg)       { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }

    // --- Listener registration ---
    public void addNumberListener(int digit, ActionListener l) { numberButtons[digit].addActionListener(l); }
    public void addDotListener(ActionListener l)    { btnDot.addActionListener(l); }
    public void addAddListener(ActionListener l)    { btnAdd.addActionListener(l); }
    public void addSubListener(ActionListener l)    { btnSub.addActionListener(l); }
    public void addMulListener(ActionListener l)    { btnMul.addActionListener(l); }
    public void addDivListener(ActionListener l)    { btnDiv.addActionListener(l); }
    public void addEqualsListener(ActionListener l) { btnEquals.addActionListener(l); }
    public void addClearListener(ActionListener l)  { btnClear.addActionListener(l); }
}
