package com.importorder.boundary;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

final class WindowState {

    private WindowState() {
    }

    static void applyMaximized(Stage stage) {
        stage.setResizable(true);
        stage.setIconified(false);
        stage.setFullScreen(false);

        Runnable enforceWindowState = () -> {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            stage.setResizable(true);
            stage.setIconified(false);
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(Math.max(720, bounds.getHeight() - 2));
        };

        enforceWindowState.run();
        Platform.runLater(enforceWindowState);

        PauseTransition secondPass = new PauseTransition(Duration.millis(120));
        secondPass.setOnFinished(event -> enforceWindowState.run());
        secondPass.play();

        PauseTransition thirdPass = new PauseTransition(Duration.millis(320));
        thirdPass.setOnFinished(event -> enforceWindowState.run());
        thirdPass.play();
    }
}
