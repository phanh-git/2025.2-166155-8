package com.importorder.boundary;

import com.importorder.controller.OverseasOrderController;
import com.importorder.dto.SaleRequestDTO;
import com.importorder.dto.SaleRequestItemDTO;
import com.importorder.dto.SiteOrderDTO;
import com.importorder.dto.SiteOrderItemDTO;
import com.importorder.entity.Site;
import com.importorder.entity.SiteMerchandise;
import com.importorder.entity.SiteOrder;
import com.importorder.repository.MerchandiseRepository;
import com.importorder.repository.SiteMerchandiseRepository;
import com.importorder.repository.SiteRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class SiteOrderCreationDialog {

    private final Stage owner;
    private final SaleRequestDTO saleRequest;
    private final OverseasOrderController overseasOrderController;
    private final SiteRepository siteRepository;
    private final SiteMerchandiseRepository siteMerchandiseRepository;
    private final MerchandiseRepository merchandiseRepository;

    SiteOrderCreationDialog(Stage owner,
                            SaleRequestDTO saleRequest,
                            OverseasOrderController overseasOrderController,
                            SiteRepository siteRepository,
                            SiteMerchandiseRepository siteMerchandiseRepository,
                            MerchandiseRepository merchandiseRepository) {
        this.owner = owner;
        this.saleRequest = saleRequest;
        this.overseasOrderController = overseasOrderController;
        this.siteRepository = siteRepository;
        this.siteMerchandiseRepository = siteMerchandiseRepository;
        this.merchandiseRepository = merchandiseRepository;
    }

    Optional<List<SiteOrderDTO>> showDialog() {
        Dialog<List<SiteOrderDTO>> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Them yeu cau dat hang");

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setPrefWidth(980);

        TextField searchField = new TextField();
        searchField.setPromptText("Tìm site theo mã hoặc tên...");
        Button searchButton = new Button("Tìm site");
        ListView<Site> siteList = new ListView<>();
        siteList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Site item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCode() + " - " + item.getName());
                }
            }
        });
        siteList.setPrefHeight(150);

        VBox draftContainer = new VBox(12);
        List<SiteOrderDraft> drafts = new ArrayList<>();

        Runnable searchSites = () -> {
            try {
                String keyword = searchField.getText().trim().toLowerCase();
                List<Site> sites = siteRepository.findAll();
                if (!keyword.isBlank()) {
                    sites = sites.stream()
                            .filter(site -> site.getCode().toLowerCase().contains(keyword)
                                    || site.getName().toLowerCase().contains(keyword))
                            .toList();
                }
                siteList.getItems().setAll(sites);
            } catch (Exception ex) {
                siteList.getItems().clear();
            }
        };

        Button addSiteButton = new Button("Thêm site vào đơn");
        addSiteButton.setOnAction(e -> {
            Site site = siteList.getSelectionModel().getSelectedItem();
            if (site == null) {
                return;
            }
            boolean exists = drafts.stream().anyMatch(draft -> draft.site.getCode().equals(site.getCode()));
            if (exists) {
                return;
            }
            try {
                SiteOrderDraft draft = new SiteOrderDraft(site, buildAvailableRequestItems(site.getCode()));
                drafts.add(draft);
                draftContainer.getChildren().add(buildDraftCard(draft, drafts, draftContainer));
            } catch (Exception ignored) {
            }
        });

        searchButton.setOnAction(e -> searchSites.run());
        searchField.setOnAction(e -> searchSites.run());
        searchSites.run();

        HBox searchRow = new HBox(10, searchField, searchButton, addSiteButton);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        ScrollPane draftScroll = new ScrollPane(draftContainer);
        draftScroll.setFitToWidth(true);
        draftScroll.setPrefHeight(320);

        Button cancelButton = new Button("Đóng");
        Button confirmButton = new Button("Xác nhận đặt hàng");
        confirmButton.getStyleClass().add("btn-primary");
        Label hint = new Label("Có thể tạo nhiều đơn đặt hàng cho nhiều site trong cùng một lần.");
        hint.getStyleClass().add("login-hint");

        HBox footer = new HBox(10, hint, new Region(), cancelButton, confirmButton);
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);
        footer.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(
                new Label("Tìm site cần đặt hàng"),
                searchRow,
                siteList,
                new Label("Danh sách đơn đặt hàng sẽ tạo"),
                draftScroll,
                footer
        );

        ButtonType hiddenCancelType = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType hiddenConfirmType = new ButtonType("Xác nhận đặt hàng", ButtonBar.ButtonData.OK_DONE);
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
                try {
                    List<SiteOrderDTO> createdOrders = new ArrayList<>();
                    for (SiteOrderDraft draft : drafts) {
                        List<SiteOrderItemDTO> items = draft.toDtos();
                        if (items.isEmpty()) {
                            continue;
                        }
                        createdOrders.add(overseasOrderController.createManualSiteOrder(
                                saleRequest.getId(),
                                draft.site.getCode(),
                                draft.deliveryMeansBox.getValue(),
                                draft.estimatedDatePicker.getValue(),
                                items
                        ));
                    }
                    return createdOrders.isEmpty() ? null : createdOrders;
                } catch (Exception ex) {
                    hint.setText("Không thể tạo đơn đặt hàng: " + ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefWidth(1020);
        confirmButton.setOnAction(e -> {
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

    private List<SaleRequestItemDTO> buildAvailableRequestItems(String siteCode) throws Exception {
        List<SiteMerchandise> siteMerchandises = siteMerchandiseRepository.findBySiteCode(siteCode);
        Set<String> availableCodes = new HashSet<>();
        for (SiteMerchandise siteMerchandise : siteMerchandises) {
            availableCodes.add(siteMerchandise.getMerchandiseCode());
        }
        return saleRequest.getItems().stream()
                .filter(item -> availableCodes.contains(item.getMerchandiseCode()))
                .toList();
    }

    private Node buildDraftCard(SiteOrderDraft draft,
                                List<SiteOrderDraft> drafts,
                                VBox container) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dbeafe; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label title = new Label(draft.site.getCode() + " - " + draft.site.getName());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;");

        ComboBox<SaleRequestItemDTO> itemPicker = new ComboBox<>();
        itemPicker.setPrefWidth(420);
        itemPicker.setCellFactory(saleRequestItemCell());
        itemPicker.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SaleRequestItemDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getMerchandiseCode() + " - " + safeName(item)
                            + " | Cần nhập: " + item.getQuantityOrdered() + " " + safeUnit(item));
                }
            }
        });
        itemPicker.getItems().setAll(draft.availableItems);

        Button addItemButton = new Button("Thêm sản phẩm");
        addItemButton.setOnAction(e -> {
            SaleRequestItemDTO selected = itemPicker.getValue();
            if (selected == null) {
                return;
            }
            boolean exists = draft.rows.stream().anyMatch(row -> row.item.getId() == selected.getId());
            if (exists) {
                return;
            }
            DraftRow row = new DraftRow(selected);
            draft.rows.add(row);
            draft.rowsBox.getChildren().add(buildDraftRowNode(draft, row));
        });

        draft.deliveryMeansBox = new ComboBox<>();
        draft.deliveryMeansBox.getItems().setAll(SiteOrder.DeliveryMeans.values());
        draft.deliveryMeansBox.setValue(SiteOrder.DeliveryMeans.SHIP_DELIVERY);
        draft.deliveryMeansBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SiteOrder.DeliveryMeans item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : UiTexts.deliveryMeans(item));
            }
        });
        draft.deliveryMeansBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SiteOrder.DeliveryMeans item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : UiTexts.deliveryMeans(item));
            }
        });

        draft.estimatedDatePicker = new DatePicker(LocalDate.now().plusDays(draft.site.getDeliveryDaysByShip()));
        draft.rowsBox = new VBox(8);

        Button removeSiteButton = new Button("Bỏ site này");
        removeSiteButton.setOnAction(e -> {
            drafts.remove(draft);
            container.getChildren().remove(card);
        });

        HBox topTools = new HBox(10,
                field("Hình thức vận chuyển", draft.deliveryMeansBox),
                field("Ngày dự kiến nhận", draft.estimatedDatePicker),
                removeSiteButton
        );

        HBox itemAdder = new HBox(10, itemPicker, addItemButton);
        itemAdder.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(title, topTools, itemAdder, draft.rowsBox);
        return card;
    }

    private Node buildDraftRowNode(SiteOrderDraft draft, DraftRow row) {
        HBox line = new HBox(10);
        line.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(row.item.getMerchandiseCode() + " - " + safeName(row.item));
        name.setPrefWidth(280);

        row.quantitySpinner = new Spinner<>(1, 100000, 1);
        row.quantitySpinner.setEditable(true);
        row.unitField = new TextField(row.item.getUnit());
        row.unitField.setPrefWidth(110);

        Button removeButton = new Button("Bỏ");
        removeButton.setOnAction(e -> {
            draft.rows.remove(row);
            draft.rowsBox.getChildren().remove(line);
        });

        line.getChildren().addAll(
                name,
                field("Số lượng", row.quantitySpinner),
                field("Đơn vị", row.unitField),
                removeButton
        );
        return line;
    }

    private javafx.util.Callback<ListView<SaleRequestItemDTO>, ListCell<SaleRequestItemDTO>> saleRequestItemCell() {
        return list -> new ListCell<>() {
            @Override
            protected void updateItem(SaleRequestItemDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getMerchandiseCode() + " - " + safeName(item)
                            + " | Cần nhập: " + item.getQuantityOrdered() + " " + safeUnit(item));
                }
            }
        };
    }

    private VBox field(String label, Node node) {
        VBox box = new VBox(6);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        box.getChildren().addAll(lbl, node);
        return box;
    }

    private String safeName(SaleRequestItemDTO item) {
        if (item.getMerchandiseName() != null && !item.getMerchandiseName().isBlank()) {
            return item.getMerchandiseName();
        }
        try {
            return merchandiseRepository.findByCode(item.getMerchandiseCode())
                    .map(merchandise -> merchandise.getName())
                    .orElse("Chưa có tên");
        } catch (Exception ex) {
            return "Chưa có tên";
        }
    }

    private String safeUnit(SaleRequestItemDTO item) {
        return item.getUnit() == null || item.getUnit().isBlank() ? "Chưa có" : item.getUnit();
    }

    private static final class SiteOrderDraft {
        private final Site site;
        private final List<SaleRequestItemDTO> availableItems;
        private final List<DraftRow> rows = new ArrayList<>();
        private ComboBox<SiteOrder.DeliveryMeans> deliveryMeansBox;
        private DatePicker estimatedDatePicker;
        private VBox rowsBox;

        private SiteOrderDraft(Site site, List<SaleRequestItemDTO> availableItems) {
            this.site = site;
            this.availableItems = availableItems;
        }

        private List<SiteOrderItemDTO> toDtos() {
            List<SiteOrderItemDTO> items = new ArrayList<>();
            for (DraftRow row : rows) {
                SiteOrderItemDTO dto = new SiteOrderItemDTO();
                dto.setSaleRequestItemId(row.item.getId());
                dto.setMerchandiseCode(row.item.getMerchandiseCode());
                dto.setMerchandiseName(row.item.getMerchandiseName());
                dto.setQuantity(row.quantitySpinner.getValue());
                dto.setUnit(row.unitField.getText().trim());
                items.add(dto);
            }
            return items;
        }
    }

    private static final class DraftRow {
        private final SaleRequestItemDTO item;
        private Spinner<Integer> quantitySpinner;
        private TextField unitField;

        private DraftRow(SaleRequestItemDTO item) {
            this.item = item;
        }
    }
}
