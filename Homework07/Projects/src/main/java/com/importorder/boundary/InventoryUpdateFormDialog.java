package com.importorder.boundary;

import com.importorder.dto.InventoryUpdateRequestDTO;
import com.importorder.entity.SiteMerchandise;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Map;
import java.util.Optional;

final class InventoryUpdateFormDialog {

    private final Stage owner;
    private final InventoryUpdateRequestDTO request;
    private final Map<String, SiteMerchandise> currentInventoryByCode;

    InventoryUpdateFormDialog(Stage owner,
                              InventoryUpdateRequestDTO request,
                              Map<String, SiteMerchandise> currentInventoryByCode) {
        this.owner = owner;
        this.request = request;
        this.currentInventoryByCode = currentInventoryByCode;
    }

    Optional<SiteMerchandise> showDialog() {
        Dialog<SiteMerchandise> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Cập nhật tồn kho theo yêu cầu");

        SiteMerchandise existing = currentInventoryByCode.get(request.getMerchandiseCode());
        RowState row = new RowState(request, existing);

        VBox root = new VBox(16);
        root.getStyleClass().add("dialog-root-card");
        root.setPadding(new Insets(22));
        root.setPrefWidth(760);

        Label title = new Label("Cập nhật tồn kho cho mặt hàng mới");
        title.getStyleClass().add("detail-title");
        Label subtitle = new Label("Site vui lòng nhập số lượng hiện có cho mặt hàng vừa được thêm vào danh mục chung.");
        subtitle.getStyleClass().add("login-hint");

        VBox contentCard = new VBox(10);
        contentCard.getStyleClass().add("detail-chip");
        contentCard.setPadding(new Insets(14));

        Label itemTitle = new Label(request.getMerchandiseCode() + " - " + safe(request.getMerchandiseName()));
        itemTitle.getStyleClass().add("detail-chip-title");
        Label hint = new Label("Đơn vị mặc định: " + safe(request.getMerchandiseUnit())
                + " | Tình trạng hiện có: " + (existing == null ? "Chưa có trong site" : "Đã có trong site"));
        hint.getStyleClass().add("login-hint");

        HBox fields = new HBox(12,
                field("Tồn kho hiện tại", row.quantitySpinner),
                field("Đơn vị", row.unitField)
        );
        fields.setAlignment(Pos.CENTER_LEFT);

        contentCard.getChildren().addAll(itemTitle, hint, fields);

        Button cancelButton = new Button("Đóng");
        cancelButton.getStyleClass().add("btn-outline");
        Button submitButton = new Button("Gửi cập nhật tồn kho");
        submitButton.getStyleClass().add("btn-primary");

        HBox footer = new HBox(10, cancelButton, submitButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, subtitle, contentCard, footer);

        ButtonType hiddenCancelType = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType hiddenConfirmType = new ButtonType("Gửi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(hiddenCancelType, hiddenConfirmType);

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

        dialog.setResultConverter(buttonType -> {
            if (buttonType != hiddenConfirmType) {
                return null;
            }
            SiteMerchandise merchandise = new SiteMerchandise();
            merchandise.setSiteCode(request.getSiteCode());
            merchandise.setMerchandiseCode(request.getMerchandiseCode());
            merchandise.setQuantity(row.quantitySpinner.getValue());
            String unitValue = row.unitField.getText() != null ? row.unitField.getText().trim() : "";
            merchandise.setUnit(unitValue.isBlank() ? row.defaultUnit() : unitValue);
            return merchandise;
        });

        submitButton.setOnAction(e -> {
            if (hiddenConfirm instanceof Button button) {
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
        box.getChildren().addAll(lbl, node);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Chưa có" : value;
    }

    private static final class RowState {
        private final InventoryUpdateRequestDTO request;
        private final Spinner<Integer> quantitySpinner;
        private final TextField unitField;

        private RowState(InventoryUpdateRequestDTO request, SiteMerchandise existing) {
            this.request = request;
            this.quantitySpinner = new Spinner<>(0, 1_000_000, existing != null ? existing.getQuantity() : 0);
            this.quantitySpinner.setEditable(true);
            this.unitField = new TextField(existing != null && existing.getUnit() != null && !existing.getUnit().isBlank()
                    ? existing.getUnit()
                    : defaultUnit());
        }

        private String defaultUnit() {
            return request.getMerchandiseUnit() == null ? "" : request.getMerchandiseUnit();
        }
    }
}
