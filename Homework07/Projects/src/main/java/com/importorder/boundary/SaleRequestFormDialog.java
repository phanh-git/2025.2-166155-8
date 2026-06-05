package com.importorder.boundary;

import com.importorder.dto.SaleRequestItemDTO;
import com.importorder.entity.Merchandise;
import com.importorder.repository.MerchandiseRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class SaleRequestFormDialog {

    private final Stage owner;
    private final MerchandiseRepository merchandiseRepository;

    SaleRequestFormDialog(Stage owner, MerchandiseRepository merchandiseRepository) {
        this.owner = owner;
        this.merchandiseRepository = merchandiseRepository;
    }

    Optional<List<SaleRequestItemDTO>> showDialog() {
        Dialog<List<SaleRequestItemDTO>> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Tạo yêu cầu nhập hàng mới");

        VBox root = new VBox(16);
        root.getStyleClass().add("dialog-root-card");
        root.setPadding(new Insets(22));
        root.setPrefWidth(940);

        TextField keywordField = new TextField();
        keywordField.setPromptText("Tìm kiếm tên mặt hàng...");
        Button searchButton = new Button("Tìm kiếm");
        searchButton.getStyleClass().add("btn-outline");

        ListView<Merchandise> resultList = new ListView<>();
        resultList.getStyleClass().add("dialog-list");
        resultList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Merchandise item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCode() + " - " + item.getName() + " (" + item.getUnit() + ")");
                }
            }
        });
        resultList.setPrefHeight(160);

        VBox selectedBox = new VBox(10);
        selectedBox.getStyleClass().add("dialog-selected-box");
        List<SelectedRequestItem> selectedItems = new ArrayList<>();

        Runnable doSearch = () -> {
            try {
                String keyword = keywordField.getText().trim();
                if (keyword.isBlank()) {
                    resultList.getItems().setAll(merchandiseRepository.findAll());
                } else {
                    resultList.getItems().setAll(merchandiseRepository.findByNameContaining(keyword));
                }
            } catch (Exception ex) {
                resultList.getItems().clear();
            }
        };

        Button addMerchandiseButton = new Button("Thêm mặt hàng vào danh sách");
        addMerchandiseButton.getStyleClass().add("btn-primary");
        addMerchandiseButton.setOnAction(e -> {
            Merchandise merchandise = resultList.getSelectionModel().getSelectedItem();
            if (merchandise == null) {
                return;
            }
            boolean exists = selectedItems.stream().anyMatch(it -> it.merchandise.getCode().equals(merchandise.getCode()));
            if (exists) {
                return;
            }
            SelectedRequestItem selected = new SelectedRequestItem(merchandise);
            selectedItems.add(selected);
            selectedBox.getChildren().add(buildSelectedRow(selected, selectedItems, selectedBox));
        });

        searchButton.setOnAction(e -> doSearch.run());
        keywordField.setOnAction(e -> doSearch.run());
        doSearch.run();

        HBox searchRow = new HBox(10, keywordField, searchButton, addMerchandiseButton);
        HBox.setHgrow(keywordField, Priority.ALWAYS);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        ScrollPane selectedPane = new ScrollPane(selectedBox);
        selectedPane.setFitToWidth(true);
        selectedPane.setPrefHeight(240);
        selectedPane.getStyleClass().add("edge-to-edge");

        Button cancelButton = new Button("Đóng");
        cancelButton.getStyleClass().add("btn-outline");
        Button submitButton = new Button("Xác nhận tạo yêu cầu");
        submitButton.getStyleClass().add("btn-primary");

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getChildren().addAll(cancelButton, submitButton);

        VBox header = new VBox(6);
        Label title = new Label("Tạo yêu cầu nhập hàng mới");
        title.getStyleClass().add("detail-title");
        Label subtitle = new Label("Tìm mặt hàng, thêm vào danh sách và nhập số lượng, đơn vị, ngày mong muốn.");
        subtitle.getStyleClass().add("login-hint");
        header.getChildren().addAll(title, subtitle);

        VBox searchCard = new VBox(10, new Label("Tìm mặt hàng"), searchRow, resultList);
        searchCard.getStyleClass().add("dialog-section-card");
        VBox selectedCard = new VBox(10, new Label("Danh sách mặt hàng sẽ gửi trong yêu cầu"), selectedPane);
        selectedCard.getStyleClass().add("dialog-section-card");

        root.getChildren().addAll(header, searchCard, selectedCard, footer);

        ButtonType hiddenCancelType = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType hiddenConfirmType = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
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
            if (buttonType == hiddenConfirmType) {
                List<SaleRequestItemDTO> items = new ArrayList<>();
                for (SelectedRequestItem selected : selectedItems) {
                    SaleRequestItemDTO dto = selected.toDto();
                    if (dto != null) {
                        items.add(dto);
                    }
                }
                return items.isEmpty() ? null : items;
            }
            return null;
        });

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefWidth(900);
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
        return dialog.showAndWait();
    }

    private Node buildSelectedRow(SelectedRequestItem selected,
                                  List<SelectedRequestItem> selectedItems,
                                  VBox selectedBox) {
        VBox wrapper = new VBox(8);
        wrapper.setPadding(new Insets(12));
        wrapper.getStyleClass().add("detail-chip");

        Label title = new Label(selected.merchandise.getCode() + " - " + selected.merchandise.getName());
        title.getStyleClass().add("detail-chip-title");

        HBox fields = new HBox(10);
        fields.setAlignment(Pos.CENTER_LEFT);

        Spinner<Integer> quantity = new Spinner<>(1, 100000, 1);
        quantity.setEditable(true);
        quantity.getStyleClass().add("form-field");
        TextField unitField = new TextField(selected.merchandise.getUnit());
        unitField.setPromptText("Đơn vị");
        unitField.getStyleClass().add("form-field");
        DatePicker desiredDate = new DatePicker(LocalDate.now().plusDays(7));
        Button removeButton = new Button("Bỏ");
        removeButton.getStyleClass().add("btn-outline");

        selected.quantity = quantity;
        selected.unitField = unitField;
        selected.desiredDate = desiredDate;

        removeButton.setOnAction(e -> {
            selectedItems.remove(selected);
            selectedBox.getChildren().remove(wrapper);
        });

        fields.getChildren().addAll(
                field("Số lượng", quantity),
                field("Đơn vị", unitField),
                field("Ngày mong muốn", desiredDate),
                removeButton
        );

        wrapper.getChildren().addAll(title, fields);
        return wrapper;
    }

    private VBox field(String label, Node node) {
        VBox box = new VBox(6);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        box.getChildren().addAll(lbl, node);
        return box;
    }

    private static final class SelectedRequestItem {
        private final Merchandise merchandise;
        private Spinner<Integer> quantity;
        private TextField unitField;
        private DatePicker desiredDate;

        private SelectedRequestItem(Merchandise merchandise) {
            this.merchandise = merchandise;
        }

        private SaleRequestItemDTO toDto() {
            if (quantity == null || desiredDate == null) {
                return null;
            }
            SaleRequestItemDTO dto = new SaleRequestItemDTO();
            dto.setMerchandiseCode(merchandise.getCode());
            dto.setQuantityOrdered(quantity.getValue());
            dto.setUnit(unitField != null ? unitField.getText().trim() : merchandise.getUnit());
            dto.setDesiredDeliveryDate(desiredDate.getValue());
            return dto;
        }
    }
}
