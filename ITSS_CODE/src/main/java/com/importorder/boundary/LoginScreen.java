package com.importorder.boundary;

import com.importorder.app.AppContext;
import com.importorder.entity.User;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.materialfx.enums.FloatMode;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class LoginScreen {

    private final Stage stage;
    private final AppContext context;

    public LoginScreen(Stage stage, AppContext context) {
        this.stage = stage;
        this.context = context;
    }

    public void show() {
        HBox root = new HBox();
        root.setPrefSize(1180, 760);

        StackPane banner = buildBanner();
        StackPane formPane = buildFormPane();
        formPane.setPrefWidth(460);
        formPane.setMinWidth(460);

        HBox.setHgrow(banner, Priority.ALWAYS);
        root.getChildren().addAll(banner, formPane);

        Scene scene = new Scene(root);
        MaterialTheme.apply(scene);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

        stage.setTitle("Import Order System");
        stage.setScene(scene);
        stage.setMinWidth(1080);
        stage.setMinHeight(700);
        stage.show();
        WindowState.applyMaximized(stage);
    }

    private StackPane buildBanner() {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("login-banner");

        // Decorative circles
        Circle c1 = new Circle(200);
        Circle c2 = new Circle(130);
        Circle c3 = new Circle(75);
        c1.getStyleClass().add("banner-circle-1");
        c2.getStyleClass().add("banner-circle-2");
        c3.getStyleClass().add("banner-circle-3");

        StackPane.setAlignment(c1, Pos.BOTTOM_LEFT);
        StackPane.setMargin(c1, new Insets(0, 0, -100, -100));
        StackPane.setAlignment(c2, Pos.TOP_RIGHT);
        StackPane.setMargin(c2, new Insets(-50, -50, 0, 0));
        StackPane.setAlignment(c3, Pos.CENTER_RIGHT);
        StackPane.setMargin(c3, new Insets(0, 60, 150, 0));

        // Content
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(480);
        content.setPadding(new Insets(0, 40, 0, 40));

        // Logo badge
        StackPane badge = new StackPane();
        Rectangle bg = new Rectangle(64, 64);
        bg.setArcHeight(18);
        bg.setArcWidth(18);
        bg.setFill(Color.web("#ffffff", 0.20));
        Label logoText = new Label("IO");
        logoText.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: white;");
        badge.getChildren().addAll(bg, logoText);
        badge.setMaxWidth(64);

        // Main title — dùng VBox để wrap nhiều dòng thay vì 1 Label dài
        Label titleLine1 = new Label("HỆ THỐNG");
        titleLine1.getStyleClass().add("banner-title");
        Label titleLine2 = new Label("ĐẶT HÀNG");
        titleLine2.getStyleClass().add("banner-title");
        Label titleLine3 = new Label("NHẬP KHẨU");
        titleLine3.getStyleClass().add("banner-title");

        content.getChildren().addAll(badge, titleLine1, titleLine2, titleLine3);
        pane.getChildren().addAll(c1, c2, c3, content);

        FadeTransition fade = new FadeTransition(Duration.millis(700), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(700), content);
        slide.setFromX(-24);
        slide.setToX(0);
        fade.play();
        slide.play();
        return pane;
    }

    private Label feature(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("banner-feature");
        label.setWrapText(true);
        return label;
    }

    private StackPane buildFormPane() {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("login-form-pane");

        VBox form = new VBox(0);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setMaxWidth(360);

        Label title = new Label("Đăng nhập");
        title.getStyleClass().add("login-title");

        Label hint = new Label("Chọn nhanh role test hoặc nhập tài khoản seed trong database.");
        hint.getStyleClass().add("login-hint");
        hint.setWrapText(true);
        VBox.setMargin(hint, new Insets(8, 0, 28, 0));

        // --- Preset combo ---
        Label presetLabel = new Label("Chọn tài khoản mẫu");
        presetLabel.getStyleClass().add("field-label");

        MFXFilterComboBox<String> presetBox = new MFXFilterComboBox<>();
        presetBox.getStyleClass().add("form-field");
        presetBox.setMaxWidth(Double.MAX_VALUE);
        // Dùng DISABLED để không có floating text che label bên trên
        presetBox.setFloatMode(FloatMode.DISABLED);
        presetBox.setPromptText("Chọn tài khoản theo vai trò...");
        Map<String, String[]> presets = demoPresets();
        presetBox.getItems().addAll(presets.keySet());
        VBox.setMargin(presetBox, new Insets(6, 0, 20, 0));

        // --- Username ---
        Label userLbl = new Label("Username");
        userLbl.getStyleClass().add("field-label");

        MFXTextField userField = new MFXTextField();
        userField.getStyleClass().add("login-field");
        // DISABLED: không dùng FloatMode.BORDER để tránh text float đè label
        userField.setFloatMode(FloatMode.DISABLED);
        userField.setPromptText("Ví dụ: sales");
        userField.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(userField, new Insets(6, 0, 20, 0));

        // --- Password ---
        Label passLbl = new Label("Password");
        passLbl.getStyleClass().add("field-label");

        MFXPasswordField passField = new MFXPasswordField();
        passField.getStyleClass().add("login-field");
        passField.setFloatMode(FloatMode.DISABLED);
        passField.setPromptText("Ví dụ: sales");
        passField.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(passField, new Insets(6, 0, 10, 0));

        // --- Error ---
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        VBox.setMargin(errorLabel, new Insets(0, 0, 12, 0));

        // --- Login button ---
        MFXButton loginBtn = new MFXButton("Đăng nhập →");
        loginBtn.setButtonType(ButtonType.RAISED);
        loginBtn.getStyleClass().add("login-btn");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        // --- Footer note ---
        Label note = new Label("Seed mặc định: admin/admin · sales/sales · overseas/overseas · warehouse/warehouse · site_jp/site_jp");
        note.getStyleClass().add("login-footer");
        note.setWrapText(true);
        VBox.setMargin(note, new Insets(22, 0, 0, 0));

        // --- Event handlers ---
        presetBox.setOnAction(e -> fillPreset(presetBox, presets, userField, passField));

        Runnable doLogin = () -> {
            errorLabel.setVisible(false);
            String username = userField.getText().trim();
            String password = passField.getText().trim();
            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Vui lòng nhập đầy đủ username và password.");
                errorLabel.setVisible(true);
                shake(form);
                return;
            }
            try {
                Optional<User> userOpt = context.authenticate(username, password);
                if (userOpt.isEmpty()) {
                    errorLabel.setText("Sai thông tin đăng nhập hoặc tài khoản chưa tồn tại trong DB.");
                    errorLabel.setVisible(true);
                    shake(form);
                    return;
                }
                User user = userOpt.get();
                context.setCurrentUser(user);
                new MainDashboard(stage, context).show();
            } catch (SQLException ex) {
                errorLabel.setText("Không kết nối được database: " + ex.getMessage());
                errorLabel.setVisible(true);
                shake(form);
            }
        };

        loginBtn.setOnAction(e -> doLogin.run());
        passField.setOnAction(e -> doLogin.run());

        form.getChildren().addAll(
            title, 
            presetLabel, presetBox,
            userLbl, userField,
            passLbl, passField,
            errorLabel, loginBtn
        );

        pane.getChildren().add(form);

        FadeTransition fade = new FadeTransition(Duration.millis(700), form);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
        return pane;
    }

    private Map<String, String[]> demoPresets() {
        Map<String, String[]> presets = new LinkedHashMap<>();
        presets.put("Quản trị hệ thống", new String[]{"admin", "admin"});
        presets.put("Bộ phận bán hàng", new String[]{"sales", "sales"});
        presets.put("Bộ phận đặt hàng quốc tế", new String[]{"overseas", "overseas"});
        presets.put("Quản lý kho", new String[]{"warehouse", "warehouse"});
        presets.put("Quản lý site JP01", new String[]{"site_jp", "site_jp"});
        return presets;
    }

    private void fillPreset(MFXFilterComboBox<String> presetBox,
                            Map<String, String[]> presets,
                            MFXTextField userField,
                            MFXPasswordField passField) {
        String selected = presetBox.getValue();
        if (selected == null) {
            selected = presetBox.getItems().isEmpty() ? null : presetBox.getItems().get(0);
            presetBox.setValue(selected);
        }
        if (selected == null) return;
        String[] pair = presets.get(selected);
        if (pair == null) return;
        userField.setText(pair[0]);
        passField.setText(pair[1]);
    }

    private void shake(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setFromX(0);
        tt.setByX(8);
        tt.setAutoReverse(true);
        tt.setCycleCount(6);
        tt.play();
    }
}
