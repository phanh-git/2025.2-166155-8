package com.importorder.boundary;

import com.importorder.entity.Site;
import com.importorder.entity.SiteUser;
import com.importorder.entity.User;
import com.importorder.repository.SiteRepository;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

final class UserFormDialog {

    private final Stage owner;
    private final SiteRepository siteRepository;

    UserFormDialog(Stage owner, SiteRepository siteRepository) {
        this.owner = owner;
        this.siteRepository = siteRepository;
    }

    Optional<UserFormResult> showDialog(User existingUser, SiteUser existingSiteUser) {
        Dialog<UserFormResult> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(existingUser == null ? "Tạo tài khoản mới" : "Cập nhật tài khoản");

        TextField usernameField = new TextField(existingUser != null ? existingUser.getUsername() : "");
        PasswordField passwordField = new PasswordField();
        passwordField.setText(existingUser != null ? existingUser.getPassword() : "");
        ComboBox<User.Role> roleBox = new ComboBox<>(FXCollections.observableArrayList(User.Role.values()));
        roleBox.setValue(existingUser != null ? existingUser.getRole() : User.Role.SALES_DEPARTMENT);
        roleBox.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(User.Role item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : UiTexts.role(item));
            }
        });
        roleBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(User.Role item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : UiTexts.role(item));
            }
        });

        ComboBox<Site> siteBox = new ComboBox<>();
        siteBox.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Site item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getCode() + " - " + item.getName());
            }
        });
        siteBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Site item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getCode() + " - " + item.getName());
            }
        });

        try {
            List<Site> sites = siteRepository.findAll();
            siteBox.getItems().setAll(sites);
            if (existingSiteUser != null) {
                sites.stream()
                        .filter(site -> site.getCode().equals(existingSiteUser.getSiteCode()))
                        .findFirst()
                        .ifPresent(siteBox::setValue);
            }
        } catch (Exception ignored) {
        }

        roleBox.valueProperty().addListener((obs, old, selected) -> siteBox.setDisable(selected != User.Role.SITE_USER));
        siteBox.setDisable(roleBox.getValue() != User.Role.SITE_USER);

        VBox root = new VBox(16);
        root.getStyleClass().add("dialog-root-card");
        root.setPadding(new Insets(22));
        root.setPrefWidth(620);

        Label title = new Label(existingUser == null ? "Tạo tài khoản mới" : "Cập nhật tài khoản");
        title.getStyleClass().add("detail-title");
        Label subtitle = new Label("Thiết lập thông tin đăng nhập, vai trò và site quản lý nếu đây là tài khoản site.");
        subtitle.getStyleClass().add("login-hint");
        subtitle.setWrapText(true);

        VBox infoCard = new VBox(12,
                field("Tên đăng nhập", usernameField),
                field("Mật khẩu", passwordField),
                field("Vai trò", roleBox),
                field("Gắn với site", siteBox)
        );
        infoCard.getStyleClass().add("dialog-section-card");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        Button cancelButton = new Button("Đóng");
        cancelButton.getStyleClass().add("btn-outline");
        Button saveButton = new Button(existingUser == null ? "Tạo tài khoản" : "Lưu thay đổi");
        saveButton.getStyleClass().add("btn-primary");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(10, spacer, cancelButton, saveButton);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        root.getChildren().addAll(title, subtitle, infoCard, errorLabel, footer);

        ButtonType hiddenCancelType = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType hiddenSaveType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(hiddenCancelType, hiddenSaveType);

        Node hiddenCancel = dialog.getDialogPane().lookupButton(hiddenCancelType);
        if (hiddenCancel != null) {
            hiddenCancel.setManaged(false);
            hiddenCancel.setVisible(false);
        }
        Node hiddenSave = dialog.getDialogPane().lookupButton(hiddenSaveType);
        if (hiddenSave != null) {
            hiddenSave.setManaged(false);
            hiddenSave.setVisible(false);
        }

        dialog.setResultConverter(buttonType -> {
            if (buttonType != hiddenSaveType) {
                return null;
            }

            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            User.Role role = roleBox.getValue();
            errorLabel.setVisible(false);
            if (username.isBlank()) {
                errorLabel.setText("Tên đăng nhập không được để trống.");
                errorLabel.setVisible(true);
                return null;
            }
            if (password.isBlank()) {
                errorLabel.setText("Mật khẩu không được để trống.");
                errorLabel.setVisible(true);
                return null;
            }
            if (role == User.Role.SITE_USER && siteBox.getValue() == null) {
                errorLabel.setText("Vui lòng chọn site cho tài khoản quản lý site.");
                errorLabel.setVisible(true);
                return null;
            }

            User user = existingUser != null ? existingUser : new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setRole(role);
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(LocalDateTime.now());
            }

            SiteUser siteUser = null;
            if (role == User.Role.SITE_USER && siteBox.getValue() != null) {
                siteUser = new SiteUser(user.getId(), siteBox.getValue().getCode());
            }
            return new UserFormResult(user, siteUser);
        });

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefWidth(620);
        saveButton.setOnAction(e -> {
            if (hiddenSave instanceof Button button) {
                button.fire();
            }
        });
        cancelButton.setOnAction(e -> {
            if (hiddenCancel instanceof Button button) {
                button.fire();
            } else {
                dialog.close();
            }
        });
        return dialog.showAndWait();
    }

    private VBox field(String labelText, Node fieldNode) {
        VBox box = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        if (fieldNode instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        box.getChildren().addAll(label, fieldNode);
        return box;
    }

    static final class UserFormResult {
        private final User user;
        private final SiteUser siteUser;

        UserFormResult(User user, SiteUser siteUser) {
            this.user = user;
            this.siteUser = siteUser;
        }

        User getUser() {
            return user;
        }

        SiteUser getSiteUser() {
            return siteUser;
        }
    }
}
