package com.importorder.boundary;

import com.importorder.entity.Merchandise;
import com.importorder.entity.SiteMerchandise;
import com.importorder.repository.MerchandiseRepository;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Optional;

public class SiteMerchandiseFormDialog {

    private final Stage owner;
    private final MerchandiseRepository merchandiseRepository;
    private final String siteCode;
    private final SiteMerchandise editingValue;

    public SiteMerchandiseFormDialog(Stage owner,
                                     MerchandiseRepository merchandiseRepository,
                                     String siteCode,
                                     SiteMerchandise editingValue) {
        this.owner = owner;
        this.merchandiseRepository = merchandiseRepository;
        this.siteCode = siteCode;
        this.editingValue = editingValue;
    }

    public Optional<SiteMerchandise> showDialog() {
        Dialog<SiteMerchandise> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(editingValue == null ? "Thêm sản phẩm cho site" : "Cập nhật sản phẩm của site");

        DialogPane pane = dialog.getDialogPane();
        if (!owner.getScene().getStylesheets().isEmpty()) {
            pane.getStylesheets().add(owner.getScene().getStylesheets().get(0));
        }
        pane.getStyleClass().add("dialog-pane");
        ButtonType saveType = new ButtonType(editingValue == null ? "Thêm sản phẩm" : "Lưu thay đổi", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeType = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(saveType, closeType);

        Label heading = new Label(editingValue == null ? "Thêm sản phẩm" : "Cập nhật sản phẩm");
        heading.getStyleClass().add("detail-title");
        Label intro = new Label(editingValue == null
                ? "Tìm kiếm sản phẩm theo mã hoặc tên rồi nhập số lượng và đơn vị tham khảo."
                : "Cập nhật số lượng và đơn vị tham khảo cho sản phẩm của site.");
        intro.getStyleClass().add("login-hint");
        intro.setWrapText(true);

        VBox content = new VBox(14);
        content.setPadding(new Insets(18));

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        TableView<Merchandise> resultTable = new TableView<>();
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        resultTable.setFixedCellSize(42);
        TableColumn<Merchandise, String> codeCol = new TableColumn<>("Mã sản phẩm");
        codeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCode()));
        TableColumn<Merchandise, String> nameCol = new TableColumn<>("Tên sản phẩm");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        TableColumn<Merchandise, String> unitCol = new TableColumn<>("Đơn vị mặc định");
        unitCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(safe(data.getValue().getUnit())));
        resultTable.getColumns().setAll(codeCol, nameCol, unitCol);
        resultTable.setPrefHeight(220);

        TextField searchField = new TextField();
        searchField.setPromptText("Nhập mã hoặc tên sản phẩm");
        Button searchButton = new Button("Tìm kiếm");
        searchButton.getStyleClass().add("btn-outline");

        TextField quantityField = new TextField();
        quantityField.setPromptText("Nhập số lượng");
        TextField unitField = new TextField();
        unitField.setPromptText("Nhập đơn vị");

        if (editingValue == null) {
            HBox searchRow = new HBox(10, searchField, searchButton);
            HBox.setHgrow(searchField, Priority.ALWAYS);
            content.getChildren().addAll(
                    heading,
                    intro,
                    field("Tìm kiếm sản phẩm", searchRow),
                    resultTable
            );

            Runnable runSearch = () -> {
                try {
                    resultTable.setItems(FXCollections.observableArrayList(
                            merchandiseRepository.searchByKeyword(searchField.getText())));
                } catch (SQLException ex) {
                    showError(errorLabel, "Không tìm được sản phẩm: " + ex.getMessage());
                }
            };

            searchButton.setOnAction(event -> runSearch.run());
            searchField.setOnAction(event -> runSearch.run());
            runSearch.run();

            resultTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selectedValue) -> {
                if (selectedValue != null && unitField.getText().trim().isBlank()) {
                    unitField.setText(safe(selectedValue.getUnit()));
                }
            });
        } else {
            Optional<Merchandise> merchandise = findMerchandise(editingValue.getMerchandiseCode());
            VBox selectedCard = new VBox(6);
            selectedCard.getStyleClass().add("detail-chip");
            selectedCard.setPadding(new Insets(12));

            Label codeValue = new Label(editingValue.getMerchandiseCode());
            codeValue.getStyleClass().add("detail-chip-title");
            Label nameValue = new Label(merchandise.map(Merchandise::getName).orElse("Không có tên sản phẩm"));
            nameValue.getStyleClass().add("login-hint");
            selectedCard.getChildren().addAll(codeValue, nameValue);

            content.getChildren().addAll(heading, intro, field("Sản phẩm", selectedCard));
            quantityField.setText(String.valueOf(editingValue.getQuantity()));
            unitField.setText(safe(editingValue.getUnit()));
        }

        content.getChildren().addAll(
                field("Số lượng tham khảo", quantityField),
                field("Đơn vị", unitField),
                errorLabel
        );

        pane.setContent(content);

        Button saveButton = (Button) pane.lookupButton(saveType);
        Button cancelButton = (Button) pane.lookupButton(closeType);
        saveButton.getStyleClass().add("btn-primary");
        cancelButton.getStyleClass().add("btn-outline");

        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                Merchandise selectedMerchandise = editingValue == null
                        ? resultTable.getSelectionModel().getSelectedItem()
                        : findMerchandise(editingValue.getMerchandiseCode()).orElse(null);
                if (selectedMerchandise == null) {
                    showError(errorLabel, "Vui lòng chọn một sản phẩm trong danh sách.");
                    event.consume();
                    return;
                }

                int quantity = Integer.parseInt(quantityField.getText().trim());
                if (quantity < 0) {
                    showError(errorLabel, "Số lượng tham khảo không được âm.");
                    event.consume();
                    return;
                }

                String unit = unitField.getText().trim().isBlank()
                        ? safe(selectedMerchandise.getUnit())
                        : unitField.getText().trim();
                if (unit.isBlank()) {
                    showError(errorLabel, "Vui lòng nhập đơn vị cho sản phẩm.");
                    event.consume();
                    return;
                }
                hideError(errorLabel);
            } catch (NumberFormatException ex) {
                showError(errorLabel, "Số lượng tham khảo phải là số nguyên hợp lệ.");
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveType) {
                return null;
            }

            Merchandise selectedMerchandise = editingValue == null
                    ? resultTable.getSelectionModel().getSelectedItem()
                    : findMerchandise(editingValue.getMerchandiseCode()).orElse(null);
            if (selectedMerchandise == null) {
                return null;
            }

            SiteMerchandise value = new SiteMerchandise();
            value.setSiteCode(siteCode);
            value.setMerchandiseCode(selectedMerchandise.getCode());
            value.setQuantity(Integer.parseInt(quantityField.getText().trim()));
            value.setUnit(unitField.getText().trim().isBlank()
                    ? safe(selectedMerchandise.getUnit())
                    : unitField.getText().trim());
            return value;
        });

        return dialog.showAndWait();
    }

    private Optional<Merchandise> findMerchandise(String merchandiseCode) {
        try {
            return merchandiseRepository.findByCode(merchandiseCode);
        } catch (SQLException ex) {
            return Optional.empty();
        }
    }

    private VBox field(String label, javafx.scene.Node node) {
        VBox box = new VBox(6);
        Label title = new Label(label);
        title.getStyleClass().add("field-label");
        if (node instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        box.getChildren().addAll(title, node);
        return box;
    }

    private static void showError(Label label, String message) {
        label.setText(message);
        label.setManaged(true);
        label.setVisible(true);
    }

    private static void hideError(Label label) {
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
