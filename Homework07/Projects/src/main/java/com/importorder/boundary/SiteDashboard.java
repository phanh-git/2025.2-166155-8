package com.importorder.boundary;

import com.importorder.app.AppContext;
import com.importorder.controller.OverseasOrderController;
import com.importorder.dto.InventoryUpdateRequestDTO;
import com.importorder.dto.SiteOrderDTO;
import com.importorder.dto.SiteOrderItemDTO;
import com.importorder.entity.Merchandise;
import com.importorder.entity.Site;
import com.importorder.entity.SiteMerchandise;
import com.importorder.entity.SiteOrder;
import com.importorder.entity.SiteUser;
import com.importorder.repository.MerchandiseRepository;
import com.importorder.repository.SiteMerchandiseRepository;
import com.importorder.repository.SiteRepository;
import com.importorder.repository.SiteUserRepository;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SiteDashboard extends DashboardBase {

    private final OverseasOrderController overseasOrderController;
    private final SiteRepository siteRepository = new SiteRepository();
    private final SiteMerchandiseRepository siteMerchandiseRepository = new SiteMerchandiseRepository();
    private final SiteUserRepository siteUserRepository = new SiteUserRepository();
    private final MerchandiseRepository merchandiseRepository = new MerchandiseRepository();

    public SiteDashboard(Stage stage, AppContext context) {
        super(stage, context);
        this.overseasOrderController = context.getOverseasOrderController();
    }

    @Override
    protected VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(250);

        VBox brand = brandPane("Hệ thống đặt hàng nhập khẩu", "Quản lý site");
        VBox nav = new VBox(6);
        nav.setPadding(new Insets(18, 14, 18, 14));
        nav.getChildren().addAll(
                navButton("Site của tôi", this::buildMySitePage),
                navButton("Yêu cầu cập nhật tồn kho", this::buildInventoryUpdateRequestsPage),
                navButton("Quản lý đơn đặt hàng", this::buildOrdersPage),
                navButton("Danh sách sản phẩm", this::buildMerchandisePage)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(brand, new Separator(), nav, spacer, sidebarUserBox());
        return sidebar;
    }

    @Override
    protected void setInitialPage() {
        setPage(buildOrdersPage());
    }

    private String getMySiteCode() throws SQLException {
        Optional<SiteUser> siteUser = siteUserRepository.findByUserId(currentUser.getId());
        return siteUser.map(SiteUser::getSiteCode)
                .orElseThrow(() -> new IllegalStateException("Tài khoản này chưa được gán với site nào"));
    }

    private Node buildOrdersPage() {
        VBox page = pageShell("Quản lý đơn đặt hàng", "Theo doi don dat hang cua site va cap nhat trang thai van chuyen.");

        TableView<SiteOrderDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<SiteOrderDTO, String> idCol = new TableColumn<>("Mã đơn");
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper("#" + data.getValue().getId()));
        TableColumn<SiteOrderDTO, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiTexts.siteOrderStatus(data.getValue().getStatus())));
        table.getColumns().addAll(idCol, statusCol);

        VBox detailBox = new VBox(10);
        detailBox.getStyleClass().add("card");
        detailBox.setPadding(new Insets(18));

        Runnable loadOrders = () -> {
            try {
                List<SiteOrderDTO> orders = overseasOrderController.getMySiteOrders();
                table.setItems(FXCollections.observableArrayList(orders));
                if (!orders.isEmpty()) {
                    table.getSelectionModel().selectFirst();
                    renderOrderDetail(detailBox, orders.get(0), true);
                } else {
                    detailBox.getChildren().setAll(paragraph("Chưa có đơn đặt hàng nào được gửi đến site của bạn."));
                }
            } catch (Exception ex) {
                detailBox.getChildren().setAll(paragraph("Không tải được đơn đặt hàng: " + ex.getMessage()));
            }
        };

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                renderOrderDetail(detailBox, selected, true);
            }
        });

        page.getChildren().addAll(table, sectionTitle("Chi tiết đơn đặt hàng"), detailBox);
        loadOrders.run();
        return scroll(page);
    }

    private void renderOrderDetail(VBox detailBox, SiteOrderDTO order, boolean allowUpdateStatus) {
        detailBox.getChildren().clear();
        detailBox.getChildren().addAll(
                new Label("Đơn đặt hàng #" + order.getId()),
                paragraph("Trạng thái: " + UiTexts.siteOrderStatus(order.getStatus())),
                paragraph("Ngày dự kiến nhận: " + order.getEstimatedDeliveryDate()),
                paragraph("Hình thức vận chuyển: " + UiTexts.deliveryMeans(order.getDeliveryMeans()))
        );

        VBox itemsBox = new VBox(8);
        for (SiteOrderItemDTO item : order.getItems()) {
            itemsBox.getChildren().add(paragraph(item.getMerchandiseCode() + " - "
                    + safe(item.getMerchandiseName()) + " - "
                    + item.getQuantity() + " " + safe(item.getUnit())));
        }
        detailBox.getChildren().add(itemsBox);

        if (allowUpdateStatus
                && order.getStatus() != SiteOrder.Status.CANCELLED
                && order.getStatus() != SiteOrder.Status.SHORTAGE_REPORTED
                && order.getStatus() != SiteOrder.Status.WAREHOUSE_CONFIRMED) {
            ComboBox<SiteOrder.Status> statusBox = new ComboBox<>();
            statusBox.getItems().setAll(SiteOrder.Status.IN_TRANSIT, SiteOrder.Status.DELIVERED);
            statusBox.setValue(order.getStatus() == SiteOrder.Status.DELIVERED
                    ? SiteOrder.Status.DELIVERED
                    : SiteOrder.Status.IN_TRANSIT);
            statusBox.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(SiteOrder.Status item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : UiTexts.siteOrderStatus(item));
                }
            });
            statusBox.setButtonCell(new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(SiteOrder.Status item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : UiTexts.siteOrderStatus(item));
                }
            });
            Button updateButton = new Button("Cập nhật trạng thái");
            updateButton.getStyleClass().add("btn-primary");
            updateButton.setOnAction(e -> {
                try {
                    overseasOrderController.updateDeliveryStatus(order.getId(), statusBox.getValue());
                    showInfo("Cập nhật thành công", "Đơn hàng đã được cập nhật trạng thái.");
                    renderOrderDetail(detailBox, reloadMyOrder(order.getId()), true);
                } catch (Exception ex) {
                    showError("Không thể cập nhật trạng thái", ex.getMessage());
                }
            });
            detailBox.getChildren().addAll(field("Trạng thái mới", statusBox), updateButton);

            if (order.getStatus() == SiteOrder.Status.ORDER_RECEIVED) {
                Button cancelButton = new Button("Báo hủy đơn này");
                cancelButton.getStyleClass().add("btn-outline");
                cancelButton.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Báo hủy đơn hàng");
                    dialog.setHeaderText("Nhập lý do hủy để gửi lại cho bộ phận đặt hàng quốc tế");
                    dialog.setContentText("Lý do:");
                    dialog.showAndWait().ifPresent(reason -> {
                        try {
                            overseasOrderController.cancelMySiteOrder(order.getId(), reason);
                            showInfo("Đã báo hủy", "Đơn đặt hàng đã được chuyển sang trạng thái hủy.");
                            renderOrderDetail(detailBox, reloadMyOrder(order.getId()), true);
                        } catch (Exception ex) {
                            showError("Không thể hủy đơn hàng", ex.getMessage());
                        }
                    });
                });
                detailBox.getChildren().add(cancelButton);
            }
        }

        if (order.getStatus() == SiteOrder.Status.CANCELLED && order.getCancelReason() != null && !order.getCancelReason().isBlank()) {
            detailBox.getChildren().add(paragraph("Lý do hủy: " + order.getCancelReason()));
        }
        if ((order.getStatus() == SiteOrder.Status.WAREHOUSE_CONFIRMED || order.getStatus() == SiteOrder.Status.SHORTAGE_REPORTED)
                && order.getItems() != null) {
            for (SiteOrderItemDTO item : order.getItems()) {
                if (item.getReceivedQuantity() != null || (item.getShortageNote() != null && !item.getShortageNote().isBlank())) {
                    detailBox.getChildren().add(paragraph(
                            item.getMerchandiseCode() + ": kho ghi nhận "
                                    + (item.getReceivedQuantity() != null ? item.getReceivedQuantity() : item.getQuantity())
                                    + "/" + item.getQuantity() + " " + safe(item.getUnit())
                                    + ((item.getShortageNote() != null && !item.getShortageNote().isBlank())
                                    ? " | Ghi chú: " + item.getShortageNote()
                                    : "")
                    ));
                }
            }
        }
    }

    private SiteOrderDTO reloadMyOrder(int orderId) throws SQLException {
        return overseasOrderController.getMySiteOrders().stream()
                .filter(order -> order.getId() == orderId)
                .findFirst()
                .orElseThrow();
    }

    private Node buildMySitePage() {
        VBox page = pageShell("Site của tôi", "Cập nhật thông tin vận chuyển của site.");

        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));

        try {
            String siteCode = getMySiteCode();
            Site site = siteRepository.findByCode(siteCode)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin site"));

            TextField nameField = new TextField(site.getName());
            TextField shipDaysField = new TextField(String.valueOf(site.getDeliveryDaysByShip()));
            TextField airDaysField = new TextField(String.valueOf(site.getDeliveryDaysByAir()));
            TextField noteField = new TextField(site.getNote());

            Button saveButton = new Button("Lưu thông tin site");
            saveButton.getStyleClass().add("btn-primary");
            saveButton.setOnAction(e -> {
                try {
                    site.setName(nameField.getText().trim());
                    site.setDeliveryDaysByShip(Integer.parseInt(shipDaysField.getText().trim()));
                    site.setDeliveryDaysByAir(Integer.parseInt(airDaysField.getText().trim()));
                    site.setNote(noteField.getText().trim());
                    siteRepository.update(site);
                    showInfo("Lưu thành công", "Thông tin site đã được cập nhật.");
                } catch (Exception ex) {
                    showError("Không thể lưu thông tin site", ex.getMessage());
                }
            });

            card.getChildren().addAll(
                    paragraph("Mã site: " + site.getCode()),
                    field("Tên site", nameField),
                    field("Số ngày vận chuyển bằng tàu", shipDaysField),
                    field("Số ngày vận chuyển bằng hàng không", airDaysField),
                    field("Ghi chú", noteField),
                    saveButton
            );
        } catch (Exception ex) {
            card.getChildren().add(paragraph("Không tải được thông tin site: " + ex.getMessage()));
        }

        page.getChildren().add(card);
        return scroll(page);
    }

    private Node buildInventoryUpdateRequestsPage() {
        VBox page = pageShell("Yêu cầu cập nhật tồn kho", "Sale gửi yêu cầu để site rà soát lại tồn kho cho các mặt hàng mới vừa thêm vào danh mục.");

        Label pendingValue = new Label("0");
        Label completedValue = new Label("0");

        VBox listBox = new VBox(12);

        final Runnable[] reloadRef = new Runnable[1];
        reloadRef[0] = () -> {
            listBox.getChildren().clear();
            try {
                List<InventoryUpdateRequestDTO> requests = overseasOrderController.getMyInventoryUpdateRequests();
                long pendingCount = requests.stream().filter(r -> r.getStatus() == com.importorder.entity.InventoryUpdateRequest.Status.PENDING).count();
                long completedCountValue = requests.size() - pendingCount;
                pendingValue.setText(String.valueOf(pendingCount));
                completedValue.setText(String.valueOf(completedCountValue));

                if (requests.isEmpty()) {
                    listBox.getChildren().add(paragraph("Hiện chưa có yêu cầu cập nhật tồn kho nào được gửi đến site của bạn."));
                    return;
                }

                Map<String, SiteMerchandise> currentInventory = siteMerchandiseRepository.findBySiteCode(getMySiteCode()).stream()
                        .collect(Collectors.toMap(SiteMerchandise::getMerchandiseCode, item -> item, (left, right) -> left, LinkedHashMap::new));

                for (InventoryUpdateRequestDTO request : requests) {
                    VBox card = new VBox(10);
                    card.getStyleClass().add("highlight-card");
                    Label title = new Label(request.getMerchandiseCode() + " - " + safe(request.getMerchandiseName()));
                    title.getStyleClass().add("detail-title");
                    Label subtitle = new Label("Site: " + request.getSiteCode() + " - " + safe(request.getSiteName()));
                    subtitle.getStyleClass().add("login-hint");
                    HBox meta = new HBox(8,
                            buildStatusChip(request),
                            chip("Gửi lúc: " + (request.getRequestedAt() != null ? request.getRequestedAt().toLocalDate() : "Chưa có"), "#eff6ff", "#1d4ed8"));
                    card.getChildren().addAll(title, subtitle, meta);

                    VBox items = new VBox(6);
                    SiteMerchandise current = currentInventory.get(request.getMerchandiseCode());
                    items.getChildren().add(paragraph(request.getMerchandiseCode() + " - "
                            + safe(request.getMerchandiseName())
                            + " | đơn vị mặc định " + safe(request.getMerchandiseUnit())
                            + " | tồn tham khảo hiện tại " + (current != null ? current.getQuantity() : 0)
                            + " " + safe(current != null ? current.getUnit() : request.getMerchandiseUnit())));
                    card.getChildren().add(items);

                    if (request.getStatus() == com.importorder.entity.InventoryUpdateRequest.Status.PENDING) {
                        Button updateButton = new Button("Cập nhật tồn kho cho mặt hàng này");
                        updateButton.getStyleClass().add("btn-primary");
                        updateButton.setOnAction(e -> {
                            InventoryUpdateFormDialog dialog = new InventoryUpdateFormDialog(stage, request, currentInventory);
                            dialog.showDialog().ifPresent(updatedInventory -> {
                                try {
                                    overseasOrderController.completeInventoryUpdateRequest(request.getId(), updatedInventory);
                                    showInfo("Đã gửi phản hồi", "Tồn kho của site đã được cập nhật cho yêu cầu này.");
                                    reloadRef[0].run();
                                } catch (Exception ex) {
                                    showError("Không thể cập nhật tồn kho", ex.getMessage());
                                }
                            });
                        });
                        card.getChildren().add(updateButton);
                    } else if (request.getRespondedAt() != null) {
                        card.getChildren().add(chip("Đã phản hồi lúc: " + request.getRespondedAt().toLocalDate(), "#dcfce7", "#166534"));
                    }

                    listBox.getChildren().add(card);
                }
            } catch (Exception ex) {
                pendingValue.setText("0");
                completedValue.setText("0");
                listBox.getChildren().setAll(paragraph("Không tải được yêu cầu cập nhật tồn kho: " + ex.getMessage()));
            }
        };

        HBox stats = new HBox(12,
                infoCard("Đang chờ phản hồi", "0", "#dc2626"),
                infoCard("Đã xử lý", "0", "#16a34a"));
        ((Label) ((VBox) stats.getChildren().get(0)).getChildren().get(1)).textProperty().bind(pendingValue.textProperty());
        ((Label) ((VBox) stats.getChildren().get(1)).getChildren().get(1)).textProperty().bind(completedValue.textProperty());

        page.getChildren().addAll(stats, listBox);
        reloadRef[0].run();
        return scroll(page);
    }

    private Node buildMerchandisePage() {
        VBox page = pageShell("Danh sách sản phẩm", "Quản lý thông tin tồn kho tham khảo để bên đặt hàng có cơ sở lựa chọn site.");

        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));

        try {
            String siteCode = getMySiteCode();
            Map<String, Merchandise> merchandiseCatalog = new LinkedHashMap<>();
            for (Merchandise merchandise : merchandiseRepository.findAll()) {
                merchandiseCatalog.put(merchandise.getCode(), merchandise);
            }

            TableView<SiteMerchandise> table = new TableView<>();
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            table.setFixedCellSize(50);
            TableColumn<SiteMerchandise, String> codeCol = new TableColumn<>("Mã sản phẩm");
            codeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getMerchandiseCode()));
            TableColumn<SiteMerchandise, String> nameCol = new TableColumn<>("Tên sản phẩm");
            nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                    merchandiseCatalog.containsKey(data.getValue().getMerchandiseCode())
                            ? safe(merchandiseCatalog.get(data.getValue().getMerchandiseCode()).getName())
                            : "Không tìm thấy tên sản phẩm"
            ));
            TableColumn<SiteMerchandise, String> quantityCol = new TableColumn<>("Số lượng tham khảo");
            quantityCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getQuantity())));
            TableColumn<SiteMerchandise, String> unitCol = new TableColumn<>("Đơn vị");
            unitCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(safe(data.getValue().getUnit())));
            TableColumn<SiteMerchandise, String> actionCol = new TableColumn<>("Hành động");
            actionCol.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
                private final Button editButton = new Button("Sửa");
                private final Button deleteButton = new Button("Xóa");
                private final HBox box = new HBox(8, editButton, deleteButton);

                {
                    editButton.getStyleClass().add("btn-outline");
                    deleteButton.getStyleClass().add("btn-outline");

                    editButton.setOnAction(event -> {
                        SiteMerchandise selected = getTableRow().getItem();
                        if (selected == null) {
                            return;
                        }
                        SiteMerchandiseFormDialog dialog = new SiteMerchandiseFormDialog(stage, merchandiseRepository, siteCode, selected);
                        dialog.showDialog().ifPresent(updatedValue -> {
                            try {
                                siteMerchandiseRepository.update(updatedValue);
                                reloadSiteMerchandiseTable(table, siteCode);
                                showInfo("Cập nhật thành công", "Thông tin sản phẩm của site đã được cập nhật.");
                            } catch (Exception ex) {
                                showError("Không thể cập nhật sản phẩm", ex.getMessage());
                            }
                        });
                    });

                    deleteButton.setOnAction(event -> {
                        SiteMerchandise selected = getTableRow().getItem();
                        if (selected == null) {
                            return;
                        }
                        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmDialog.initOwner(stage);
                        confirmDialog.setTitle("Xác nhận xóa sản phẩm");
                        confirmDialog.setHeaderText("Bạn có chắc muốn xóa sản phẩm khỏi site không?");
                        String merchandiseName = merchandiseCatalog.containsKey(selected.getMerchandiseCode())
                                ? safe(merchandiseCatalog.get(selected.getMerchandiseCode()).getName())
                                : "Không tìm thấy tên sản phẩm";
                        confirmDialog.setContentText(selected.getMerchandiseCode() + " - " + merchandiseName);
                        confirmDialog.showAndWait().ifPresent(result -> {
                            if (result != javafx.scene.control.ButtonType.OK) {
                                return;
                            }
                            try {
                                siteMerchandiseRepository.delete(siteCode, selected.getMerchandiseCode());
                                reloadSiteMerchandiseTable(table, siteCode);
                                showInfo("Đã xóa sản phẩm", "Sản phẩm đã được xóa khỏi danh sách của site.");
                            } catch (Exception ex) {
                                showError("Không thể xóa sản phẩm", ex.getMessage());
                            }
                        });
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
            actionCol.setCellValueFactory(data -> new ReadOnlyStringWrapper("actions"));

            table.getColumns().addAll(codeCol, nameCol, quantityCol, unitCol, actionCol);
            reloadSiteMerchandiseTable(table, siteCode);
            table.setPrefHeight(360);

            Button addButton = new Button("Thêm sản phẩm");
            addButton.getStyleClass().add("btn-primary");
            addButton.setOnAction(e -> {
                SiteMerchandiseFormDialog dialog = new SiteMerchandiseFormDialog(stage, merchandiseRepository, siteCode, null);
                dialog.showDialog().ifPresent(newValue -> {
                    try {
                        Optional<SiteMerchandise> existing = siteMerchandiseRepository.findBySiteAndMerchandise(siteCode, newValue.getMerchandiseCode());
                        if (existing.isPresent()) {
                            showError("Không thể thêm sản phẩm", "Sản phẩm này đã có trong danh sách. Hãy dùng nút Sửa để cập nhật.");
                            return;
                        }
                        siteMerchandiseRepository.insert(newValue);
                        reloadSiteMerchandiseTable(table, siteCode);
                        showInfo("Thêm sản phẩm thành công", "Sản phẩm mới đã được thêm vào site.");
                    } catch (Exception ex) {
                        showError("Không thể thêm sản phẩm", ex.getMessage());
                    }
                });
            });

            card.getChildren().addAll(addButton, table);
        } catch (Exception ex) {
            card.getChildren().add(paragraph("Không tải được sản phẩm của site: " + ex.getMessage()));
        }

        page.getChildren().add(card);
        return scroll(page);
    }

    private VBox field(String label, Node node) {
        VBox box = new VBox(6);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        box.getChildren().addAll(lbl, node);
        return box;
    }

    private void reloadSiteMerchandiseTable(TableView<SiteMerchandise> table, String siteCode) throws SQLException {
        table.setItems(FXCollections.observableArrayList(siteMerchandiseRepository.findBySiteCode(siteCode)));
    }

    private Label chip(String text, String background, String color) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: " + background + ";"
                + "-fx-text-fill: " + color + ";"
                + "-fx-background-radius: 999;"
                + "-fx-border-radius: 999;"
                + "-fx-padding: 5 10 5 10;"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: 600;");
        return label;
    }

    private Label buildStatusChip(InventoryUpdateRequestDTO request) {
        if (request.getStatus() == com.importorder.entity.InventoryUpdateRequest.Status.COMPLETED) {
            return chip("Đã cập nhật", "#dcfce7", "#166534");
        }
        return chip("Đang chờ", "#fee2e2", "#b91c1c");
    }
}
