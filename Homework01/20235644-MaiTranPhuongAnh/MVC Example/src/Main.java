public class Main {
    public static void main(String[] args) {
        StudentModel model = new StudentModel("Phuong");
        StudentView view = new StudentView();
        StudentController controller = new StudentController(model, view);

        controller.updateView();
    }
}