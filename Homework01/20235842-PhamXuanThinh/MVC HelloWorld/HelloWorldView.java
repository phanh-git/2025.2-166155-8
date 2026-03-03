import java.util.Scanner;

public class HelloWorldView {
    private Scanner scanner = new Scanner(System.in);

    public String getUserInput() {
        System.out.print("Input Name: ");
        return scanner.nextLine();
    }

    public void showMessage(String message) {
        System.out.println(message);
    }
}