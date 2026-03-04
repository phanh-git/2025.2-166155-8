

public class HelloWorldController{
    private HelloWorldView view;
    private HelloWorldModel model;

    public HelloWorldController(HelloWorldView view, HelloWorldModel model){
        this.model = model;
        this.view = view;
    }

    public void display(){
        String name = view.getUserInput();
        model.setName(name);
        view.showMessage(model.getMessage());
    }
}