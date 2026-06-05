package com.importorder.app;

import javafx.application.Application;
import javafx.stage.Stage;

import com.importorder.boundary.LoginScreen;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        AppContext context = new AppContext();
        LoginScreen login = new LoginScreen(primaryStage, context);
        login.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}