package com.importorder.boundary;

import com.importorder.entity.Merchandise;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

final class MerchandiseFormDialog {

    static final class Result {
        private final Merchandise merchandise;
        private final boolean requestInventoryUpdate;

        private Result(Merchandise merchandise, boolean requestInventoryUpdate) {
            this.merchandise = merchandise;
            this.requestInventoryUpdate = requestInventoryUpdate;
        }

        Merchandise getMerchandise() {
            return merchandise;
        }

        boolean isRequestInventoryUpdate() {
            return requestInventoryUpdate;
        }
    }

    private final Stage owner;

    MerchandiseFormDialog(Stage owner) {
        this.owner = owner;
    }

    Optional<Result> showDialog() {
        Dialog<Result> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Thêm mặt hàng mới");

        VBox root = new VBox(16);
        root.getStyleClass().add("dialog-root-card");
        root.setPadding(new Insets(22));
        root.setPrefWidth(680);

        Label title = new Label("Thêm mặt hàng mới vào danh mục");
        title.getStyleClass().add("detail-title");
        Label subtitle = new Label("Nhập tên và đơn vị mặc định. Nếu đây là mặt hàng mới, bạn có thể yêu cầu các site cập nhật tồn kho ngay tại đây.");
        subtitle.getStyleClass().add("login-hint");

        TextField nameField = new TextField();
        nameField.setPromptText("Ví dụ: Trà hoa cúc nhập khẩu");
        TextField unitField = new TextField();
        unitField.setPromptText("Ví dụ: hộp");

        Button cancelButton = new Button("Đóng");
        cancelButton.getStyleClass().add("btn-outline");
        Button submitButton = new Button("Chỉ thêm mặt hàng");
        submitButton.getStyleClass().add("btn-outline");
        Button submitAndRequestButton = new Button("Thêm và yêu cầu site cập nhật tồn kho");
        submitAndRequestButton.getStyleClass().add("btn-primary");

        HBox footer = new HBox(10, cancelButton, submitButton, submitAndRequestButton);
        footer.setFillHeight(true);

        root.getChildren().addAll(
                title,
                subtitle,
                field("Tên mặt hàng", nameField),
                field("Đơn vị mặc định", unitField),
                footer
        );

        ButtonType hiddenCancelType = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType hiddenConfirmType = new ButtonType("Thêm", ButtonBar.ButtonData.OK_DONE);
        ButtonType hiddenConfirmAndRequestType = new ButtonType("Thêm và gửi yêu cầu", ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().setAll(hiddenCancelType, hiddenConfirmType, hiddenConfirmAndRequestType);

        Node hiddenCancel = dialog.getDialogPane().lookupButton(hiddenCancelType);
        if (hiddenCancel != null) {
            hiddenCancel.setManaged(false);
            hiddenCancel.setVisible(false);
        }
        Node hiddenConfirm = dialog.getDialogPane().lookupButton(hiddenConfirmType);
        if (hiddenConfirm != null) {
            hiddenConfirm.setManaged(false);
            hiddenConfirm.setVisible(false);
        }
        Node hiddenConfirmAndRequest = dialog.getDialogPane().lookupButton(hiddenConfirmAndRequestType);
        if (hiddenConfirmAndRequest != null) {
            hiddenConfirmAndRequest.setManaged(false);
            hiddenConfirmAndRequest.setVisible(false);
        }

        dialog.setResultConverter(buttonType -> {
            if (buttonType != hiddenConfirmType && buttonType != hiddenConfirmAndRequestType) {
                return null;
            }
            Merchandise merchandise = new Merchandise();
            merchandise.setName(nameField.getText() != null ? nameField.getText().trim() : "");
            merchandise.setUnit(unitField.getText() != null ? unitField.getText().trim() : "");
            return new Result(merchandise, buttonType == hiddenConfirmAndRequestType);
        });

        submitButton.setOnAction(e -> {
            if (hiddenConfirm instanceof Button button) {
                button.fire();
            }
        });
        submitAndRequestButton.setOnAction(e -> {
            if (hiddenConfirmAndRequest instanceof Button button) {
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

        dialog.getDialogPane().setContent(root);
        return dialog.showAndWait();
    }

    private VBox field(String label, Node node) {
        VBox box = new VBox(6);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        VBox.setVgrow(node, Priority.NEVER);
        box.getChildren().addAll(lbl, node);
        return box;
    }
}
