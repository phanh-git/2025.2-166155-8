public class HelloWorldModel {
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return "Hello, " + name + "!";
    }
}