

public class main {
    public static void main(String[] args) {
        HelloWorldModel model = new HelloWorldModel();
        HelloWorldView view = new HelloWorldView();
        HelloWorldController controller = new HelloWorldController(view, model);
        controller.display();
    }
}