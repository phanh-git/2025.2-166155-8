package com.importorder.boundary;

import com.importorder.app.AppContext;
import com.importorder.controller.OverseasOrderController;
import com.importorder.dto.SiteOrderDTO;
import com.importorder.dto.SiteOrderItemDTO;
import com.importorder.entity.SiteOrder;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

public class WarehouseDashboard extends DashboardBase {

    private final OverseasOrderController overseasOrderController;

    public WarehouseDashboard(Stage stage, AppContext context) {
        super(stage, context);
        this.overseasOrderController = context.getOverseasOrderController();
    }

    @Override
    protected VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(250);
        VBox brand = brandPane("Hệ thống đặt hàng nhập khẩu", "Quản lý kho");
        VBox nav = new VBox(6);
        nav.setPadding(new Insets(18, 14, 18, 14));
        nav.getChildren().add(navButton("Quản lý đơn đặt hàng", this::buildOrdersPage));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(brand, new Separator(), nav, spacer, sidebarUserBox());
        return sidebar;
    }

    @Override
    protected void setInitialPage() {
        setPage(buildOrdersPage());
    }

    private Node buildOrdersPage() {
        VBox page = pageShell("Quản lý đơn đặt hàng", "Kiem tra hang da den va xac nhan trang thai don hang.");

        TableView<SiteOrderDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<SiteOrderDTO, String> idCol = new TableColumn<>("Mã đơn");
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper("#" + data.getValue().getId()));
        TableColumn<SiteOrderDTO, String> siteCol = new TableColumn<>("Site");
        siteCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getSiteCode()));
        TableColumn<SiteOrderDTO, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiTexts.siteOrderStatus(data.getValue().getStatus())));
        table.getColumns().addAll(idCol, siteCol, statusCol);

        VBox detailBox = new VBox(10);
        detailBox.getStyleClass().add("card");
        detailBox.setPadding(new Insets(18));

        Runnable loadOrders = () -> {
            try {
                List<SiteOrderDTO> orders = overseasOrderController.getAllSiteOrders();
                table.setItems(FXCollections.observableArrayList(orders));
                if (!orders.isEmpty()) {
                    table.getSelectionModel().selectFirst();
                    renderOrderDetail(detailBox, orders.get(0));
                } else {
                    detailBox.getChildren().setAll(paragraph("Chưa có đơn đặt hàng nào."));
                }
            } catch (Exception ex) {
                detailBox.getChildren().setAll(paragraph("Không tải được dữ liệu: " + ex.getMessage()));
            }
        };

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                renderOrderDetail(detailBox, selected);
            }
        });

        page.getChildren().addAll(table, sectionTitle("Chi tiết và xác nhận đơn hàng"), detailBox);
        loadOrders.run();
        return scroll(page);
    }

    private void renderOrderDetail(VBox detailBox, SiteOrderDTO order) {
        detailBox.getChildren().clear();
        detailBox.getChildren().addAll(
                new Label("Đơn đặt hàng #" + order.getId()),
                paragraph("Site: " + order.getSiteCode() + " - " + safe(order.getSiteName())),
                paragraph("Trạng thái hiện tại: " + UiTexts.siteOrderStatus(order.getStatus())),
                paragraph("Ngày dự kiến nhận: " + order.getEstimatedDeliveryDate()),
                paragraph("Ngày thực tế nhận: " + (order.getActualDeliveryDate() != null ? order.getActualDeliveryDate() : "Chưa cập nhật"))
        );

        VBox itemsBox = new VBox(8);
        List<WarehouseItemReportRow> reportRows = new java.util.ArrayList<>();
        for (SiteOrderItemDTO item : order.getItems()) {
            VBox row = new VBox(8);
            row.getStyleClass().add("detail-chip");
            row.setPadding(new Insets(12));
            row.getChildren().add(paragraph(item.getMerchandiseCode() + " - "
                    + safe(item.getMerchandiseName()) + " - "
                    + item.getQuantity() + " " + safe(item.getUnit())));

            if (order.getStatus() == SiteOrder.Status.WAREHOUSE_CONFIRMED
                    || order.getStatus() == SiteOrder.Status.SHORTAGE_REPORTED) {
                Integer received = item.getReceivedQuantity();
                row.getChildren().add(paragraph("Kho ghi nhận: "
                        + (received != null ? received : item.getQuantity()) + " / " + item.getQuantity() + " " + safe(item.getUnit())));
                if (item.getShortageNote() != null && !item.getShortageNote().isBlank()) {
                    row.getChildren().add(paragraph("Ghi chú thiếu hàng: " + item.getShortageNote()));
                }
            } else {
                Spinner<Integer> receivedSpinner = new Spinner<>(0, item.getQuantity(), item.getQuantity());
                receivedSpinner.setEditable(true);
                TextArea shortageNoteArea = new TextArea();
                shortageNoteArea.setPromptText("Nếu thiếu hàng, ghi rõ thiếu bao nhiêu hoặc lý do...");
                shortageNoteArea.setPrefRowCount(2);
                row.getChildren().addAll(
                        field("Số lượng kho thực nhận", receivedSpinner),
                        field("Ghi chú thiếu hàng", shortageNoteArea)
                );
                reportRows.add(new WarehouseItemReportRow(item, receivedSpinner, shortageNoteArea));
            }
            itemsBox.getChildren().add(row);
        }
        detailBox.getChildren().add(itemsBox);

        if (order.getStatus() != SiteOrder.Status.WAREHOUSE_CONFIRMED
                && order.getStatus() != SiteOrder.Status.SHORTAGE_REPORTED) {
            DatePicker actualDatePicker = new DatePicker(LocalDate.now());
            Button submitReportButton = new Button("Gửi báo cáo kiểm hàng");
            submitReportButton.getStyleClass().add("btn-primary");
            submitReportButton.setOnAction(e -> {
                try {
                    List<SiteOrderItemDTO> reports = new java.util.ArrayList<>();
                    for (WarehouseItemReportRow row : reportRows) {
                        SiteOrderItemDTO report = new SiteOrderItemDTO();
                        report.setId(row.item().getId());
                        report.setReceivedQuantity(row.receivedSpinner().getValue());
                        String note = row.shortageNoteArea().getText().trim();
                        report.setShortageNote(note.isBlank() ? null : note);
                        reports.add(report);
                    }
                    overseasOrderController.confirmWarehouseReceipt(order.getId(), actualDatePicker.getValue(), reports);
                    boolean hasShortage = reports.stream().anyMatch(report -> {
                        SiteOrderItemDTO original = order.getItems().stream()
                                .filter(item -> item.getId() == report.getId())
                                .findFirst()
                                .orElse(null);
                        return original != null && report.getReceivedQuantity() != null
                                && report.getReceivedQuantity() < original.getQuantity();
                    });
                    showInfo(
                            "Đã gửi báo cáo kiểm hàng",
                            hasShortage
                                    ? "Kho đã báo thiếu hàng cho đơn này. Bộ phận đặt hàng quốc tế và bên sale sẽ nhìn thấy báo cáo."
                                    : "Đơn hàng đã được xác nhận đủ hàng."
                    );
                    renderOrderDetail(detailBox, reloadOrder(order.getId()));
                } catch (Exception ex) {
                    showError("Không thể xác nhận đơn hàng", ex.getMessage());
                }
            });
            detailBox.getChildren().addAll(field("Ngày thực tế nhận hàng", actualDatePicker), submitReportButton);
        }
    }

    private SiteOrderDTO reloadOrder(int orderId) throws Exception {
        return overseasOrderController.getAllSiteOrders().stream()
                .filter(order -> order.getId() == orderId)
                .findFirst()
                .orElseThrow();
    }

    private VBox field(String label, Node node) {
        VBox box = new VBox(6);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        box.getChildren().addAll(lbl, node);
        return box;
    }

    private record WarehouseItemReportRow(
            SiteOrderItemDTO item,
            Spinner<Integer> receivedSpinner,
            TextArea shortageNoteArea
    ) {
    }
}
