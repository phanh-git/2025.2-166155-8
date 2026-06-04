package com.importorder.boundary;

import com.importorder.app.AppContext;
import com.importorder.entity.User;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.enums.ButtonType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Supplier;

abstract class DashboardBase {

    protected final Stage stage;
    protected final AppContext context;
    protected final User currentUser;
    protected final StackPane contentArea = new StackPane();

    protected DashboardBase(Stage stage, AppContext context) {
        this.stage = stage;
        this.context = context;
        this.currentUser = context.getCurrentUser();
    }

    public final void show() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard-root");
        root.setLeft(buildSidebar());
        root.setCenter(buildMainContent());
        setInitialPage();

        Scene scene = new Scene(root, 1320, 820);
        MaterialTheme.apply(scene);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        stage.setTitle("Hệ thống đặt hàng nhập khẩu");
        stage.setScene(scene);
        stage.setMinWidth(1180);
        stage.setMinHeight(760);
        stage.show();
        WindowState.applyMaximized(stage);
    }

    protected abstract VBox buildSidebar();

    protected abstract void setInitialPage();

    protected Node buildMainContent() {
        VBox wrapper = new VBox(0);
        wrapper.getStyleClass().add("main-content");

        HBox topbar = new HBox();
        topbar.getStyleClass().add("topbar");
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.setPadding(new Insets(0, 24, 0, 24));
        topbar.setPrefHeight(58);

        Label date = new Label(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", new Locale("vi"))));
        date.getStyleClass().add("topbar-date");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label mood = new Label(UiTexts.role(currentUser.getRole()));
        mood.getStyleClass().add("topbar-date");

        contentArea.getStyleClass().add("content-area");
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        topbar.getChildren().addAll(date, spacer, mood);
        wrapper.getChildren().addAll(topbar, contentArea);
        return wrapper;
    }

    protected VBox brandPane(String title, String subtitle) {
        VBox brand = new VBox(8);
        brand.getStyleClass().add("sidebar-logo");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("sidebar-app-name");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("sidebar-userrole");
        brand.getChildren().addAll(titleLabel, subtitleLabel);
        return brand;
    }

    protected Button navButton(String text, Supplier<Node> pageSupplier) {
        MFXButton btn = new MFXButton(text);
        btn.setButtonType(ButtonType.FLAT);
        btn.getStyleClass().add("nav-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setOnAction(e -> setPage(pageSupplier.get()));
        return btn;
    }

    protected VBox sidebarUserBox() {
        VBox userBox = new VBox(6);
        userBox.getStyleClass().add("sidebar-user");
        Label name = new Label(currentUser.getUsername());
        name.getStyleClass().add("sidebar-username");
        Label role = new Label(UiTexts.role(currentUser.getRole()));
        role.getStyleClass().add("sidebar-userrole");

        MFXButton logout = new MFXButton("Đăng xuất");
        logout.setButtonType(ButtonType.FLAT);
        logout.getStyleClass().addAll("btn-outline", "sidebar-logout-btn");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> {
            context.logout();
            new LoginScreen(stage, context).show();
        });
        userBox.getChildren().addAll(name, role, logout);
        return userBox;
    }

    protected VBox pageShell(String title, String subtitle) {
        VBox page = new VBox(20);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(28, 32, 32, 32));
        Label pageTitle = new Label(title);
        pageTitle.getStyleClass().add("page-title");
        Label pageSubtitle = new Label(subtitle);
        pageSubtitle.getStyleClass().add("login-hint");
        pageSubtitle.setWrapText(true);
        page.getChildren().addAll(pageTitle, pageSubtitle);
        return page;
    }

    protected Node scroll(Node node) {
        ScrollPane pane = new ScrollPane(node);
        pane.setFitToWidth(true);
        pane.getStyleClass().add("edge-to-edge");
        return pane;
    }

    protected Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    protected Label paragraph(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("login-hint");
        return label;
    }

    protected VBox cardWrap(Node child) {
        VBox box = new VBox(child);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));
        return box;
    }

    protected VBox infoCard(String label, String value, String color) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: " + color + "18; -fx-border-color: " + color + "33;"
                + "-fx-border-radius: 14; -fx-background-radius: 14;");
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        v.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22px;");
        card.getChildren().addAll(l, v);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    protected Button actionButton(String text, String color, Supplier<Node> pageSupplier) {
        MFXButton button = new MFXButton(text);
        button.setButtonType(ButtonType.RAISED);
        button.getStyleClass().add("action-btn");
        button.setStyle("-fx-background-color: " + color + ";");
        button.setOnAction(e -> setPage(pageSupplier.get()));
        HBox.setHgrow(button, Priority.ALWAYS);
        return button;
    }

    protected void setPage(Node node) {
        contentArea.getChildren().setAll(node);
    }

    protected void showInfo(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    protected void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    protected String safe(String value) {
        return value == null || value.isBlank() ? "Chưa có" : value;
    }
}
