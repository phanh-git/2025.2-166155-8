package com.importorder.boundary;

import com.importorder.app.AppContext;
import com.importorder.controller.OverseasOrderController;
import com.importorder.controller.SaleRequestController;
import com.importorder.dto.SaleRequestDTO;
import com.importorder.dto.SaleRequestItemDTO;
import com.importorder.dto.SiteOrderDTO;
import com.importorder.dto.SiteOrderItemDTO;
import com.importorder.entity.Merchandise;
import com.importorder.entity.SaleRequest;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.enums.ButtonType;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SalesDashboard extends DashboardBase {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SaleRequestController saleRequestController;
    private final OverseasOrderController overseasOrderController;

    public SalesDashboard(Stage stage, AppContext context) {
        super(stage, context);
        this.saleRequestController = context.getSaleRequestController();
        this.overseasOrderController = context.getOverseasOrderController();
    }

    @Override
    protected VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(250);

        VBox brand = brandPane("Hệ thống đặt hàng nhập khẩu", "Bộ phận bán hàng");
        VBox nav = new VBox(6);
        nav.setPadding(new Insets(18, 14, 18, 14));
        nav.getChildren().addAll(
                navButton("Quản lý yêu cầu nhập hàng", this::buildRequestManagementPage),
                navButton("Danh mục hàng", this::buildMerchandiseCatalogPage)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(brand, new Separator(), nav, spacer, sidebarUserBox());
        return sidebar;
    }

    @Override
    protected void setInitialPage() {
        setPage(buildRequestManagementPage());
    }

    private Node buildRequestManagementPage() {
        VBox page = pageShell("Quản lý yêu cầu nhập hàng", "Theo dõi lịch sử yêu cầu nhập hàng và tạo yêu cầu mới.");

        Label totalValue = statValueLabel();
        Label completedValue = statValueLabel();
        Label processingValue = statValueLabel();

        HBox stats = new HBox(12,
                statCard("Tổng số yêu cầu", totalValue, "#dbeafe", "#1d4ed8"),
                statCard("Đã xử lý", completedValue, "#dcfce7", "#15803d"),
                statCard("Đang xử lý", processingValue, "#fef3c7", "#b45309")
        );

        MFXButton createButton = new MFXButton("Tạo yêu cầu nhập hàng mới");
        createButton.setButtonType(ButtonType.RAISED);
        createButton.getStyleClass().add("btn-primary");

        TableView<SaleRequestDTO> requestTable = buildRequestTable();

        Runnable loadRequests = () -> {
            try {
                List<SaleRequestDTO> requests = saleRequestController.getMyRequests();
                requestTable.setItems(FXCollections.observableArrayList(requests));
                requestTable.setPrefHeight(Math.max(180, 58 + requests.size() * 46.0));
                totalValue.setText(String.valueOf(requests.size()));
                completedValue.setText(String.valueOf(countByStatus(requests, SaleRequest.Status.SUCCESS)));
                processingValue.setText(String.valueOf(countProcessingRequests(requests)));
            } catch (SQLException ex) {
                requestTable.setItems(FXCollections.observableArrayList());
                totalValue.setText("0");
                completedValue.setText("0");
                processingValue.setText("0");
                showError("Không tải được dữ liệu", ex.getMessage());
            }
        };

        createButton.setOnAction(e -> {
            SaleRequestFormDialog dialog = new SaleRequestFormDialog(stage, new com.importorder.repository.MerchandiseRepository());
            dialog.showDialog().ifPresent(items -> {
                try {
                    SaleRequestDTO created = saleRequestController.submitRequest(items);
                    showInfo("Tạo yêu cầu thành công",
                            "Đã tạo yêu cầu nhập hàng #" + created.getId() + " với " + created.getItems().size()
                                    + " mặt hàng.");
                    setPage(buildSaleRequestDetailPage(created));
                } catch (Exception ex) {
                    showError("Không thể tạo yêu cầu nhập hàng", ex.getMessage());
                }
            });
        });

        VBox heroCard = new VBox(16);
        heroCard.getStyleClass().add("hero-card");
        heroCard.getChildren().addAll(stats, createButton);

        page.getChildren().addAll(heroCard, requestTable);
        loadRequests.run();
        return scroll(page);
    }

    private TableView<SaleRequestDTO> buildRequestTable() {
        TableView<SaleRequestDTO> table = new TableView<>();
        table.getStyleClass().add("sales-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(320);
        table.setFixedCellSize(50);

        TableColumn<SaleRequestDTO, String> idCol = new TableColumn<>("Mã yêu cầu");
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper("#" + data.getValue().getId()));

        TableColumn<SaleRequestDTO, String> createdAtCol = new TableColumn<>("Ngày tạo");
        createdAtCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getCreatedAt() != null
                        ? data.getValue().getCreatedAt().format(DATE_TIME_FORMATTER)
                        : "Chưa có"));

        TableColumn<SaleRequestDTO, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                UiTexts.saleRequestStatus(data.getValue().getStatus())));

        TableColumn<SaleRequestDTO, Void> actionCol = new TableColumn<>("Hành động");
        actionCol.setMinWidth(170);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("Xem chi tiết");
            {
                viewButton.getStyleClass().add("btn-outline");
                viewButton.setMinWidth(126);
                viewButton.setPrefHeight(34);
                viewButton.setOnAction(e -> {
                    SaleRequestDTO item = getTableView().getItems().get(getIndex());
                    setPage(buildSaleRequestDetailPage(item));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });

        table.getColumns().addAll(idCol, createdAtCol, statusCol, actionCol);
        return table;
    }

    private Node buildSaleRequestDetailPage(SaleRequestDTO request) {
        VBox page = pageShell("Chi tiết yêu cầu nhập hàng", "Xem toàn bộ thông tin của yêu cầu nhập hàng đã tạo.");

        Button backButton = new Button("Quay lại danh sách");
        backButton.getStyleClass().add("btn-outline");
        backButton.setOnAction(e -> setPage(buildRequestManagementPage()));

        VBox detailBox = new VBox(14);
        detailBox.getChildren().addAll(
                buildDetailHeader(
                        "Yêu cầu nhập hàng #" + request.getId(),
                        "Ngày tạo",
                        request.getCreatedAt() != null ? request.getCreatedAt().format(DATE_TIME_FORMATTER) : "Chưa có",
                        UiTexts.saleRequestStatus(request.getStatus())
                ),
                sectionTitle("Các mặt hàng cần nhập"),
                buildRequestItemsBox(request)
        );
        Node cancellationSection = buildCancellationSection(request);
        if (cancellationSection != null) {
            detailBox.getChildren().add(cancellationSection);
        }
        detailBox.getChildren().add(buildWarehouseReportSection(request));
        page.getChildren().addAll(backButton, detailBox);
        return scroll(page);
    }

    private Node buildMerchandiseCatalogPage() {
        VBox page = pageShell("Danh mục hàng", "Quản lý danh sách mặt hàng chuẩn để sale chọn nhanh khi tạo yêu cầu nhập hàng.");

        Label totalValue = statValueLabel();
        Label unitValue = statValueLabel();

        TextField searchField = new TextField();
        searchField.setPromptText("Tìm theo mã hoặc tên mặt hàng...");
        searchField.getStyleClass().add("form-field");

        Button searchButton = new Button("Lọc danh mục");
        searchButton.getStyleClass().add("btn-outline");
        Button addButton = new Button("Thêm mặt hàng mới");
        addButton.getStyleClass().add("btn-primary");

        TableView<Merchandise> table = new TableView<>();
        table.getStyleClass().add("sales-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setFixedCellSize(50);

        TableColumn<Merchandise, String> codeCol = new TableColumn<>("Mã hàng");
        codeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCode()));
        TableColumn<Merchandise, String> nameCol = new TableColumn<>("Tên mặt hàng");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        TableColumn<Merchandise, String> unitCol = new TableColumn<>("Đơn vị mặc định");
        unitCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(safe(data.getValue().getUnit())));
        table.getColumns().addAll(codeCol, nameCol, unitCol);

        Runnable reloadTable = () -> {
            try {
                List<Merchandise> merchandises = saleRequestController.searchMerchandiseCatalog(searchField.getText());
                table.setItems(FXCollections.observableArrayList(merchandises));
                table.setPrefHeight(Math.max(240, 58 + merchandises.size() * 46.0));
                totalValue.setText(String.valueOf(merchandises.size()));
                long distinctUnits = merchandises.stream()
                        .map(Merchandise::getUnit)
                        .filter(unit -> unit != null && !unit.isBlank())
                        .distinct()
                        .count();
                unitValue.setText(String.valueOf(distinctUnits));
            } catch (Exception ex) {
                table.setItems(FXCollections.observableArrayList());
                totalValue.setText("0");
                unitValue.setText("0");
                showError("Không tải được danh mục hàng", ex.getMessage());
            }
        };

        searchButton.setOnAction(e -> reloadTable.run());
        searchField.setOnAction(e -> reloadTable.run());
        addButton.setOnAction(e -> {
            MerchandiseFormDialog dialog = new MerchandiseFormDialog(stage);
            dialog.showDialog().ifPresent(result -> {
                try {
                    Merchandise created = saleRequestController.createMerchandise(result.getMerchandise());
                    if (result.isRequestInventoryUpdate()) {
                        saleRequestController.requestSitesToUpdateInventoryForMerchandise(created.getCode());
                        showInfo("Đã thêm mặt hàng và gửi yêu cầu",
                                created.getCode() + " - " + created.getName()
                                        + " đã được thêm vào danh mục và yêu cầu cập nhật tồn kho đã gửi tới các site.");
                    } else {
                        showInfo("Đã thêm mặt hàng",
                                created.getCode() + " - " + created.getName() + " đã được thêm vào danh mục.");
                    }
                    reloadTable.run();
                } catch (Exception ex) {
                    showError("Không thể thêm mặt hàng", ex.getMessage());
                }
            });
        });

        HBox actions = new HBox(10, searchField, searchButton, addButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox hero = new VBox(16,
                new HBox(12,
                        statCard("Mặt hàng đang quản lý", totalValue, "#e0f2fe", "#0369a1"),
                        statCard("Đơn vị chuẩn đang dùng", unitValue, "#fef3c7", "#b45309")),
                actions);
        hero.getStyleClass().add("hero-card");

        page.getChildren().addAll(hero, table);
        reloadTable.run();
        return scroll(page);
    }

    private Node buildCancellationSection(SaleRequestDTO request) {
        VBox section = new VBox(12);
        section.getChildren().add(sectionTitle("Thông tin hủy yêu cầu"));

        if (request.getStatus() == SaleRequest.Status.CANCELLED) {
            section.getChildren().add(buildEmptyState(
                    request.getCancelResolutionNote() != null && !request.getCancelResolutionNote().isBlank()
                            ? request.getCancelResolutionNote()
                            : "Yêu cầu đã bị hủy."));
            return section;
        }

        if (request.getCancelResolutionNote() != null && !request.getCancelResolutionNote().isBlank()) {
            section.getChildren().add(buildEmptyState(request.getCancelResolutionNote()));
        }

        if (request.isCancelRequested()) {
            section.getChildren().add(buildEmptyState(
                    "Đã gửi yêu cầu hủy tới bộ phận đặt hàng quốc tế."
                            + (request.getCancelRequestReason() != null && !request.getCancelRequestReason().isBlank()
                            ? "\nLý do: " + request.getCancelRequestReason()
                            : "")
            ));
            return section;
        }

        if (request.getStatus() == SaleRequest.Status.SUCCESS) {
            section.getChildren().add(buildEmptyState("Yêu cầu đã hoàn thành nên không thể gửi yêu cầu hủy."));
            return section;
        }

        Button requestCancelButton = new Button("Gửi yêu cầu hủy");
        requestCancelButton.getStyleClass().add("btn-outline");
        requestCancelButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Gửi yêu cầu hủy");
            dialog.setHeaderText("Nhập lý do để gửi yêu cầu hủy tới bộ phận đặt hàng quốc tế");
            dialog.setContentText("Lý do:");
            dialog.showAndWait().ifPresent(reason -> {
                try {
                    saleRequestController.requestCancellation(request.getId(), reason);
                    showInfo("Đã gửi yêu cầu hủy", "Bộ phận đặt hàng quốc tế sẽ tiếp nhận và phản hồi yêu cầu hủy.");
                    setPage(buildSaleRequestDetailPage(saleRequestController.getRequestDetail(request.getId())));
                } catch (Exception ex) {
                    showError("Không thể gửi yêu cầu hủy", ex.getMessage());
                }
            });
        });
        section.getChildren().add(requestCancelButton);
        return section;
    }

    private Node buildWarehouseReportSection(SaleRequestDTO request) {
        VBox section = new VBox(12);
        section.getChildren().add(sectionTitle("Báo cáo từ bộ phận kho"));
        try {
            List<SiteOrderDTO> siteOrders = overseasOrderController.getSiteOrdersBySaleRequest(request.getId());
            if (siteOrders.isEmpty()) {
                section.getChildren().add(buildEmptyState("Chưa có đơn đặt hàng nào được gửi tới site."));
                return section;
            }
            boolean hasReport = false;
            for (SiteOrderDTO siteOrder : siteOrders) {
                if (siteOrder.getStatus() == com.importorder.entity.SiteOrder.Status.WAREHOUSE_CONFIRMED
                        || siteOrder.getStatus() == com.importorder.entity.SiteOrder.Status.SHORTAGE_REPORTED) {
                    hasReport = true;
                    VBox card = new VBox(8);
                    card.getStyleClass().add("detail-chip");
                    card.setPadding(new Insets(12));
                    Label title = new Label("Đơn #" + siteOrder.getId() + " - " + siteOrder.getSiteCode() + " - " + safe(siteOrder.getSiteName()));
                    title.getStyleClass().add("detail-chip-title");
                    card.getChildren().addAll(title, paragraph("Trạng thái: " + UiTexts.siteOrderStatus(siteOrder.getStatus())));
                    for (SiteOrderItemDTO item : siteOrder.getItems()) {
                        String line = item.getMerchandiseCode() + " - " + safe(item.getMerchandiseName())
                                + " | đặt " + item.getQuantity() + " " + safe(item.getUnit())
                                + " | kho nhận " + (item.getReceivedQuantity() != null ? item.getReceivedQuantity() : item.getQuantity()) + " " + safe(item.getUnit());
                        card.getChildren().add(paragraph(line));
                        if (item.getShortageNote() != null && !item.getShortageNote().isBlank()) {
                            card.getChildren().add(paragraph("Ghi chú thiếu hàng: " + item.getShortageNote()));
                        }
                    }
                    section.getChildren().add(card);
                }
            }
            if (!hasReport) {
                section.getChildren().add(buildEmptyState("Kho chưa gửi báo cáo kiểm hàng cho các đơn liên quan."));
            }
        } catch (Exception ex) {
            section.getChildren().add(buildEmptyState("Không tải được báo cáo từ kho: " + ex.getMessage()));
        }
        return section;
    }

    private VBox buildDetailHeader(String titleText, String metaLabel, String metaValue, String statusText) {
        VBox header = new VBox(10);
        header.getStyleClass().add("card");
        header.setPadding(new Insets(18));

        Label title = new Label(titleText);
        title.getStyleClass().add("detail-title");

        HBox metaRow = new HBox(10);
        metaRow.getChildren().addAll(
                chip(metaLabel + ": " + metaValue, "#dbeafe", "#1d4ed8"),
                chip("Trạng thái: " + statusText, "#ecfeff", "#0f766e")
        );

        header.getChildren().addAll(title, metaRow);
        return header;
    }

    private VBox buildRequestItemsBox(SaleRequestDTO request) {
        VBox itemsBox = new VBox(10);
        if (request.getItems() == null || request.getItems().isEmpty()) {
            itemsBox.getChildren().add(buildEmptyState("Yêu cầu này chưa có mặt hàng nào."));
            return itemsBox;
        }
        for (SaleRequestItemDTO item : request.getItems()) {
            VBox row = new VBox(4);
            row.getStyleClass().add("detail-chip");
            row.setPadding(new Insets(12));
            Label title = new Label(item.getMerchandiseCode() + " - " + safe(item.getMerchandiseName()));
            title.getStyleClass().add("detail-chip-title");
            HBox metaRow = new HBox(8);
            metaRow.getChildren().addAll(
                    chip("Số lượng: " + item.getQuantityOrdered(), "#eff6ff", "#2563eb"),
                    chip("Đơn vị: " + safe(item.getUnit()), "#f8fafc", "#475569"),
                    chip("Ngày mong muốn: " + (item.getDesiredDeliveryDate() != null ? item.getDesiredDeliveryDate() : "Chưa có"), "#f0fdf4", "#16a34a")
            );
            row.getChildren().addAll(title, metaRow);
            itemsBox.getChildren().add(row);
        }
        return itemsBox;
    }

    private VBox buildEmptyState(String message) {
        VBox box = new VBox(6);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12;");
        Label label = new Label(message);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #64748b;");
        box.getChildren().add(label);
        return box;
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

    private VBox statCard(String titleText, Label valueLabel, String background, String valueColor) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: linear-gradient(to right, #ffffff, " + background + ");"
                + "-fx-border-color: #dbeafe; -fx-border-radius: 14; -fx-background-radius: 14;");
        Label title = new Label(titleText);
        title.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-font-weight: 600;");
        valueLabel.setStyle("-fx-text-fill: " + valueColor + "; -fx-font-size: 26px; -fx-font-weight: 800;");
        card.getChildren().addAll(title, valueLabel);
        return card;
    }

    private Label statValueLabel() {
        Label label = new Label("0");
        label.setMinWidth(80);
        return label;
    }

    private int countByStatus(List<SaleRequestDTO> requests, SaleRequest.Status status) {
        return (int) requests.stream()
                .filter(request -> request.getStatus() == status)
                .count();
    }

    private int countProcessingRequests(List<SaleRequestDTO> requests) {
        return (int) requests.stream()
                .filter(request -> request.getStatus() == SaleRequest.Status.RECEIVED
                        || request.getStatus() == SaleRequest.Status.IN_PROGRESS)
                .count();
    }
}
