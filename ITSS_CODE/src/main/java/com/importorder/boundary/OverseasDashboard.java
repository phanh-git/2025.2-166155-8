package com.importorder.boundary;

import com.importorder.app.AppContext;
import com.importorder.controller.OverseasOrderController;
import com.importorder.controller.SaleRequestController;
import com.importorder.dto.SaleRequestDTO;
import com.importorder.dto.SaleRequestItemDTO;
import com.importorder.dto.SiteOrderDTO;
import com.importorder.dto.SiteOrderItemDTO;
import com.importorder.entity.SaleRequest;
import com.importorder.entity.Site;
import com.importorder.entity.SiteMerchandise;
import com.importorder.repository.MerchandiseRepository;
import com.importorder.repository.SiteMerchandiseRepository;
import com.importorder.repository.SiteRepository;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class OverseasDashboard extends DashboardBase {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SaleRequestController saleRequestController;
    private final OverseasOrderController overseasOrderController;
    private final SiteRepository siteRepository = new SiteRepository();
    private final SiteMerchandiseRepository siteMerchandiseRepository = new SiteMerchandiseRepository();
    private final MerchandiseRepository merchandiseRepository = new MerchandiseRepository();
    private final OverseasOrderSuggestionEngine suggestionEngine =
            new OverseasOrderSuggestionEngine(siteRepository, siteMerchandiseRepository);
    private final Map<Integer, PlanningState> planningStates = new HashMap<>();

    public OverseasDashboard(Stage stage, AppContext context) {
        super(stage, context);
        this.saleRequestController = context.getSaleRequestController();
        this.overseasOrderController = context.getOverseasOrderController();
    }

    @Override
    protected VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(250);

        VBox brand = brandPane("Hệ thống đặt hàng nhập khẩu", "Bộ phận đặt hàng quốc tế");

        VBox nav = new VBox(6);
        nav.setPadding(new Insets(18, 14, 18, 14));
        nav.getChildren().addAll(
                navButton("Quản lý yêu cầu nhập hàng", this::buildRequestPage),
                navButton("Quản lý đơn đặt hàng", this::buildSiteOrderPage),
                navButton("Quản lý thông tin site", this::buildSiteInfoPage)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(brand, new Separator(), nav, spacer, sidebarUserBox());
        return sidebar;
    }

    @Override
    protected void setInitialPage() {
        setPage(buildRequestPage());
    }

    private Node buildRequestPage() {
        VBox page = pageShell("Quản lý yêu cầu nhập hàng", "Xem các yêu cầu từ bộ phận bán hàng và lập yêu cầu đặt hàng tới site.");

        Label totalValue = statValueLabel();
        Label completedValue = statValueLabel();
        Label processingValue = statValueLabel();
        Label pendingValue = statValueLabel();
        Label cancelledValue = statValueLabel();

        HBox stats = new HBox(12,
                statCard("Tổng số yêu cầu", totalValue, "#ede9fe", "#7c3aed"),
                statCard("Đã xử lý", completedValue, "#dcfce7", "#15803d"),
                statCard("Đang xử lý", processingValue, "#fef3c7", "#b45309"),
                statCard("Chưa xử lý", pendingValue, "#e0f2fe", "#0369a1"),
                statCard("Đã hủy", cancelledValue, "#fee2e2", "#b91c1c")
        );
        stats.setFillHeight(true);

        VBox sections = new VBox(22);

        Runnable loadRequests = () -> {
            try {
                List<SaleRequestDTO> requests = saleRequestController.getAllRequests();
                totalValue.setText(String.valueOf(requests.size()));
                completedValue.setText(String.valueOf(countByStatus(requests, SaleRequest.Status.SUCCESS)));
                processingValue.setText(String.valueOf(countByStatus(requests, SaleRequest.Status.IN_PROGRESS)));
                pendingValue.setText(String.valueOf(countByStatus(requests, SaleRequest.Status.RECEIVED)));
                cancelledValue.setText(String.valueOf(countByStatus(requests, SaleRequest.Status.CANCELLED)));

                sections.getChildren().setAll(
                        buildRequestSection("Yêu cầu cần xử lý", filterRequests(requests, SaleRequest.Status.RECEIVED)),
                        buildRequestSection("Yêu cầu đang xử lý", filterRequests(requests, SaleRequest.Status.IN_PROGRESS)),
                        buildRequestSection("Yêu cầu đã hoàn thành", filterRequests(requests, SaleRequest.Status.SUCCESS)),
                        buildRequestSection("Yêu cầu đã hủy", filterRequests(requests, SaleRequest.Status.CANCELLED))
                );
            } catch (Exception ex) {
                totalValue.setText("0");
                completedValue.setText("0");
                processingValue.setText("0");
                pendingValue.setText("0");
                cancelledValue.setText("0");
                sections.getChildren().setAll(buildEmptyState("Không tải được danh sách yêu cầu: " + ex.getMessage()));
                showError("Không tải được dữ liệu", ex.getMessage());
            }
        };

        page.getChildren().addAll(
                stats,
                sections
        );
        loadRequests.run();
        return scroll(page);
    }

    private VBox buildRequestSection(String title, List<SaleRequestDTO> requests) {
        VBox section = new VBox(12);
        section.getChildren().add(sectionTitle(title + " (" + requests.size() + ")"));
        if (requests.isEmpty()) {
            section.getChildren().add(buildEmptyState("Không có yêu cầu nào trong nhóm này."));
            return section;
        }
        section.getChildren().add(buildRequestTable(requests));
        return section;
    }

    private TableView<SaleRequestDTO> buildRequestTable(List<SaleRequestDTO> requests) {
        TableView<SaleRequestDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(Math.max(140, 58 + requests.size() * 42.0));
        table.setItems(FXCollections.observableArrayList(requests));

        TableColumn<SaleRequestDTO, String> idCol = new TableColumn<>("Mã yêu cầu");
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper("#" + data.getValue().getId()));
        TableColumn<SaleRequestDTO, String> createdByCol = new TableColumn<>("Người tạo");
        createdByCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(requestCreatorLabel(data.getValue())));
        TableColumn<SaleRequestDTO, String> createdAtCol = new TableColumn<>("Ngày tạo");
        createdAtCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getCreatedAt() != null
                        ? data.getValue().getCreatedAt().format(DATE_TIME_FORMATTER)
                        : "Chưa có"));
        TableColumn<SaleRequestDTO, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiTexts.saleRequestStatus(data.getValue().getStatus())));
        TableColumn<SaleRequestDTO, Void> actionCol = new TableColumn<>("Hành động");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("Xem chi tiết");
            {
                viewButton.getStyleClass().add("btn-outline");
                viewButton.setOnAction(e -> {
                    SaleRequestDTO item = getTableView().getItems().get(getIndex());
                    setPage(buildRequestDetailPage(item));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });
        table.getColumns().addAll(idCol, createdByCol, createdAtCol, statusCol, actionCol);
        return table;
    }

    private Node buildRequestDetailPage(SaleRequestDTO request) {
        VBox page = pageShell("Chi tiết yêu cầu nhập hàng", "Xem yêu cầu và thực hiện hành động phù hợp theo trạng thái.");

        Button backButton = new Button("Quay lại danh sách");
        backButton.getStyleClass().add("btn-outline");
        backButton.setOnAction(e -> setPage(buildRequestPage()));

        VBox detailBox = new VBox(14);
        detailBox.getChildren().addAll(
                buildDetailHeader(
                        "Yêu cầu nhập hàng #" + request.getId(),
                        "Ngày tạo",
                        request.getCreatedAt() != null ? request.getCreatedAt().format(DATE_TIME_FORMATTER) : "Chưa có",
                        UiTexts.saleRequestStatus(request.getStatus())
                ),
                chipBlock("Người tạo: " + requestCreatorLabel(request), "#eef2ff", "#4338ca"),
                sectionTitle("Các mặt hàng cần nhập"),
                buildRequestItemsBox(request)
        );
        if (request.isCancelRequested()) {
            detailBox.getChildren().add(chipBlock(
                    "Bộ phận bán hàng đã gửi yêu cầu hủy"
                            + (request.getCancelRequestReason() != null && !request.getCancelRequestReason().isBlank()
                            ? ": " + request.getCancelRequestReason()
                            : ""),
                    "#fee2e2",
                    "#b91c1c"
            ));
        } else if (request.getCancelResolutionNote() != null && !request.getCancelResolutionNote().isBlank()
                && request.getStatus() != SaleRequest.Status.CANCELLED) {
            detailBox.getChildren().add(chipBlock(request.getCancelResolutionNote(), "#fff7ed", "#c2410c"));
        }

        detailBox.getChildren().add(buildRequestStatusSection(request));
        page.getChildren().addAll(backButton, detailBox);
        return scroll(page);
    }

    private Node buildRequestStatusSection(SaleRequestDTO request) {
        return switch (request.getStatus()) {
            case RECEIVED -> buildPendingRequestSection(request);
            case IN_PROGRESS -> buildInProgressSection(request, true);
            case SUCCESS -> buildInProgressSection(request, false);
            case CANCELLED -> buildCancelledSection(request);
        };
    }

    private Node buildPendingRequestSection(SaleRequestDTO request) {
        PlanningState state = planningStates.computeIfAbsent(request.getId(), key -> new PlanningState());

        VBox section = new VBox(16);
        section.getChildren().add(sectionTitle("Hành động xử lý"));

        HBox actions = new HBox(10);
        Button suggestButton = new Button("Gợi ý đặt hàng");
        suggestButton.getStyleClass().add("btn-primary");
        suggestButton.setOnAction(e -> {
            try {
                OverseasOrderSuggestionEngine.SuggestionResult suggestion = suggestionEngine.suggest(request);
                state.replaceWithSuggestion(suggestion);
                if (!suggestion.errors().isEmpty()) {
                    showError("Gợi ý chưa hoàn chỉnh", String.join("\n", suggestion.errors()));
                }
                refreshRequestDetailPage(reloadSaleRequest(request.getId()), true);
            } catch (Exception ex) {
                showError("Không thể gợi ý đặt hàng", ex.getMessage());
            }
        });

        Button cancelButton = new Button("Báo hủy đến bên sale");
        cancelButton.getStyleClass().add("btn-outline");
        cancelButton.setOnAction(e -> handleCancelRequest(request));
        actions.getChildren().addAll(suggestButton, cancelButton);

        if (request.isCancelRequested()) {
            Button processCancelButton = new Button("Xử lý yêu cầu hủy từ sale");
            processCancelButton.getStyleClass().add("btn-outline");
            processCancelButton.setOnAction(e -> handleProcessSaleCancellationRequest(request));
            actions.getChildren().add(processCancelButton);
        }

        section.getChildren().addAll(actions, buildPlanningWorkspace(request, state, false));
        return section;
    }

    private VBox buildPlanningWorkspace(SaleRequestDTO request, PlanningState state, boolean collapsible) {
        VBox wrapper = new VBox(14);
        wrapper.getChildren().add(buildDraftSummary(request, state));

        if (collapsible && !state.plannerVisible) {
            return wrapper;
        }

        VBox planner = new VBox(14);
        planner.getChildren().add(sectionTitle("Lập kế hoạch đặt hàng"));
        for (SaleRequestItemDTO item : request.getItems()) {
            planner.getChildren().add(buildPlanningCard(request, item, state));
        }
        wrapper.getChildren().add(planner);
        return wrapper;
    }

    private VBox buildPlanningCard(SaleRequestDTO request, SaleRequestItemDTO item, PlanningState state) {
        VBox card = new VBox(12);
        card.getStyleClass().add("detail-chip");
        card.setPadding(new Insets(14));

        int plannedQuantity = state.totalAllocatedForItem(item.getId());
        int remaining = Math.max(0, item.getQuantityOrdered() - plannedQuantity);

        Label title = new Label(item.getMerchandiseCode() + " - " + safe(item.getMerchandiseName()));
        title.getStyleClass().add("detail-chip-title");

        HBox meta = new HBox(8);
        meta.getChildren().addAll(
                chip("Cần nhập: " + item.getQuantityOrdered() + " " + safe(item.getUnit()), "#eff6ff", "#2563eb"),
                chip("Đã chọn: " + plannedQuantity + " " + safe(item.getUnit()), "#dcfce7", "#15803d"),
                chip("Còn thiếu: " + remaining + " " + safe(item.getUnit()), remaining == 0 ? "#ecfeff" : "#fef3c7", remaining == 0 ? "#0f766e" : "#b45309"),
                chip("Hạn nhận: " + (item.getDesiredDeliveryDate() != null ? item.getDesiredDeliveryDate() : "Chưa có"), "#f8fafc", "#475569")
        );

        Button toggleButton = new Button(state.expandedItemIds.contains(item.getId()) ? "Ẩn danh sách site" : "Tìm kiếm hàng trong các site");
        toggleButton.getStyleClass().add("btn-outline");
        toggleButton.setOnAction(e -> {
            state.toggleItemExpansion(item.getId());
            refreshRequestDetailPage(request, true);
        });

        VBox candidateBox = new VBox(10);
        if (state.expandedItemIds.contains(item.getId())) {
            try {
                OverseasOrderSuggestionEngine.ItemPlan itemPlan = suggestionEngine.buildItemPlan(item);
                if (itemPlan.candidates().isEmpty()) {
                    candidateBox.getChildren().add(buildEmptyState("Không có site nào đáp ứng được ngày nhận mong muốn cho mặt hàng này."));
                } else {
                    for (OverseasOrderSuggestionEngine.CandidateOption option : itemPlan.candidates()) {
                        candidateBox.getChildren().add(buildCandidateCard(request, item, option, state));
                    }
                }
            } catch (Exception ex) {
                candidateBox.getChildren().add(buildEmptyState("Không thể tải danh sách site phù hợp: " + ex.getMessage()));
            }
        }

        card.getChildren().addAll(title, meta, toggleButton, candidateBox);
        return card;
    }

    private VBox buildCandidateCard(SaleRequestDTO request,
                                    SaleRequestItemDTO item,
                                    OverseasOrderSuggestionEngine.CandidateOption option,
                                    PlanningState state) {
        VBox card = new VBox(8);
        card.getStyleClass().add("dialog-section-card");
        card.setPadding(new Insets(12));

        Label title = new Label(option.siteCode() + " - " + safe(option.siteName()));
        title.getStyleClass().add("detail-chip-title");

        int remaining = Math.max(0, item.getQuantityOrdered() - state.totalAllocatedForItem(item.getId()));
        int maxAllocatable = Math.min(remaining, option.availableQuantity());
        Spinner<Integer> quantitySpinner = new Spinner<>(1, Math.max(1, maxAllocatable), Math.max(1, maxAllocatable));
        quantitySpinner.setEditable(true);
        quantitySpinner.setDisable(maxAllocatable <= 0);

        Button addButton = new Button(maxAllocatable > 0 ? "Thêm vào giỏ hàng" : "Đã đủ số lượng");
        addButton.getStyleClass().add("btn-primary");
        addButton.setDisable(maxAllocatable <= 0);
        addButton.setOnAction(e -> {
            int quantity = Math.min(quantitySpinner.getValue(), Math.min(option.availableQuantity(), Math.max(0, item.getQuantityOrdered() - state.totalAllocatedForItem(item.getId()))));
            if (quantity <= 0) {
                return;
            }
            state.addAllocation(new PlannedAllocation(
                    item.getId(),
                    item.getMerchandiseCode(),
                    safe(item.getMerchandiseName()),
                    option.siteCode(),
                    safe(option.siteName()),
                    option.deliveryMeans(),
                    option.arrivalDate(),
                    quantity,
                    safe(item.getUnit())
            ));
            state.plannerVisible = true;
            refreshRequestDetailPage(request, true);
        });

        HBox row = new HBox(10,
                chip("Phương tiện: " + UiTexts.deliveryMeans(option.deliveryMeans()), "#eef2ff", "#4338ca"),
                chip("Dự kiến nhận: " + option.arrivalDate(), "#f0fdf4", "#15803d"),
                chip("Tồn kho: " + option.availableQuantity() + " " + safe(option.unit()), "#fff7ed", "#c2410c")
        );

        HBox actionRow = new HBox(10, field("Số lượng lấy", quantitySpinner), addButton);
        actionRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        card.getChildren().addAll(title, row, actionRow);
        return card;
    }

    private VBox buildDraftSummary(SaleRequestDTO request, PlanningState state) {
        VBox wrapper = new VBox(12);
        wrapper.getChildren().add(sectionTitle("Giỏ đơn đặt hàng theo site"));

        Map<String, SiteDraftSummary> grouped = state.groupBySiteDraft();
        if (grouped.isEmpty()) {
            wrapper.getChildren().add(buildEmptyState("Chưa có sản phẩm nào được thêm vào kế hoạch đặt hàng."));
            return wrapper;
        }

        for (SiteDraftSummary summary : grouped.values()) {
            VBox card = new VBox(10);
            card.getStyleClass().add("detail-chip");
            card.setPadding(new Insets(14));

            Label title = new Label(summary.siteCode + " - " + summary.siteName);
            title.getStyleClass().add("detail-chip-title");

            HBox meta = new HBox(8);
            meta.getChildren().addAll(
                    chip("Phương tiện: " + UiTexts.deliveryMeans(summary.deliveryMeans), "#eff6ff", "#2563eb"),
                    chip("Ngày dự kiến: " + summary.arrivalDate, "#ecfeff", "#0f766e")
            );

            VBox lines = new VBox(8);
            for (PlannedAllocation allocation : summary.allocations) {
                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label info = new Label(allocation.merchandiseCode + " - " + allocation.merchandiseName
                        + " | " + allocation.quantity + " " + allocation.unit);
                info.setStyle("-fx-text-fill: #1e3a5f; -fx-font-size: 13px; -fx-font-weight: 600;");
                info.setWrapText(true);
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Button remove = new Button("Bỏ");
                remove.getStyleClass().add("btn-outline");
                remove.setOnAction(e -> {
                    state.removeAllocation(allocation);
                    refreshRequestDetailPage(request, true);
                });
                row.getChildren().addAll(info, spacer, remove);
                lines.getChildren().add(row);
            }

            card.getChildren().addAll(title, meta, lines);
            wrapper.getChildren().add(card);
        }

        List<String> missing = validatePlanningCoverage(request, state);
        if (!missing.isEmpty()) {
            wrapper.getChildren().add(buildEmptyState("Thiếu sản phẩm: " + String.join(" | ", missing)));
        }

        Button confirmButton = new Button("Xác nhận tạo đơn");
        confirmButton.getStyleClass().add("btn-primary");
        confirmButton.setDisable(!missing.isEmpty());
        confirmButton.setOnAction(e -> handleCreateSiteOrders(request, state));
        wrapper.getChildren().add(confirmButton);
        return wrapper;
    }

    private Node buildInProgressSection(SaleRequestDTO request, boolean allowConfirm) {
        VBox section = new VBox(14);
        section.getChildren().add(sectionTitle("Theo dõi tiến trình"));

        try {
            List<SiteOrderDTO> orders = overseasOrderController.getSiteOrdersBySaleRequest(request.getId());
            if (orders.isEmpty()) {
                section.getChildren().add(buildEmptyState("Chưa có đơn đặt hàng nào được tạo cho yêu cầu này."));
                return section;
            }

            boolean readyToConfirm = isReadyToConfirmSuccess(request, orders);
            section.getChildren().add(buildProgressSummary(request, orders));
            for (SiteOrderDTO order : orders) {
                section.getChildren().add(buildProgressCard(order));
            }

            HBox actions = new HBox(10);
            Button addMoreOrderButton = new Button("Tạo thêm đơn đặt hàng");
            addMoreOrderButton.getStyleClass().add("btn-outline");
            addMoreOrderButton.setOnAction(e -> {
                PlanningState state = planningStates.computeIfAbsent(request.getId(), key -> new PlanningState());
                state.plannerVisible = !state.plannerVisible;
                refreshRequestDetailPage(request, true);
            });
            actions.getChildren().add(addMoreOrderButton);

            if (request.isCancelRequested()) {
                Button processCancelButton = new Button("Xử lý yêu cầu hủy từ sale");
                processCancelButton.getStyleClass().add("btn-outline");
                processCancelButton.setOnAction(e -> handleProcessSaleCancellationRequest(request));
                actions.getChildren().add(processCancelButton);
            }
            section.getChildren().add(actions);

            PlanningState state = planningStates.computeIfAbsent(request.getId(), key -> new PlanningState());
            if (state.plannerVisible) {
                section.getChildren().add(buildPlanningWorkspace(request, state, true));
            }

            if (allowConfirm) {
                Button confirmButton = new Button("Xác nhận yêu cầu thành công");
                confirmButton.getStyleClass().add("btn-primary");
                confirmButton.setDisable(!readyToConfirm);
                confirmButton.setOnAction(e -> {
                    try {
                        overseasOrderController.confirmSaleRequestSuccess(request.getId());
                        showInfo("Xác nhận thành công", "Yêu cầu nhập hàng đã được chuyển sang trạng thái hoàn thành.");
                        setPage(buildRequestDetailPage(reloadSaleRequest(request.getId())));
                    } catch (Exception ex) {
                        showError("Không thể xác nhận yêu cầu", ex.getMessage());
                    }
                });
                section.getChildren().add(confirmButton);
                if (!readyToConfirm) {
                    section.getChildren().add(buildEmptyState("Chỉ có thể xác nhận khi tổng số lượng kho đã nhận của từng mặt hàng đã đáp ứng đủ yêu cầu."));
                }
            }
        } catch (Exception ex) {
            section.getChildren().add(buildEmptyState("Không tải được tiến trình đơn hàng: " + ex.getMessage()));
        }
        return section;
    }

    private Node buildCancelledSection(SaleRequestDTO request) {
        VBox section = new VBox(14);
        section.getChildren().add(sectionTitle("Thông tin hủy yêu cầu"));
        String reason = request.getCancelResolutionNote() != null && !request.getCancelResolutionNote().isBlank()
                ? request.getCancelResolutionNote()
                : "Yêu cầu đã bị hủy. Chưa có lý do chi tiết được ghi nhận trong hệ thống.";
        section.getChildren().add(buildEmptyState(reason));
        return section;
    }

    private Node buildSiteOrderPage() {
        VBox page = pageShell("Quản lý đơn đặt hàng", "Theo dõi tất cả đơn đặt hàng đã gửi tới các site.");

        TableView<SiteOrderDTO> orderTable = buildSiteOrderTable();

        Runnable loadOrders = () -> {
            try {
                List<SiteOrderDTO> orders = overseasOrderController.getAllSiteOrders();
                orderTable.setItems(FXCollections.observableArrayList(orders));
            } catch (Exception ex) {
                orderTable.setItems(FXCollections.observableArrayList());
                showError("Không tải được đơn đặt hàng", ex.getMessage());
            }
        };

        page.getChildren().add(orderTable);
        loadOrders.run();
        return scroll(page);
    }

    private TableView<SiteOrderDTO> buildSiteOrderTable() {
        TableView<SiteOrderDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(300);

        TableColumn<SiteOrderDTO, String> idCol = new TableColumn<>("Mã đơn");
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper("#" + data.getValue().getId()));
        TableColumn<SiteOrderDTO, String> siteCol = new TableColumn<>("Site");
        siteCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getSiteCode() + " - " + safe(data.getValue().getSiteName())));
        TableColumn<SiteOrderDTO, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiTexts.siteOrderStatus(data.getValue().getStatus())));
        TableColumn<SiteOrderDTO, String> etaCol = new TableColumn<>("Ngày dự kiến");
        etaCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getEstimatedDeliveryDate() != null ? data.getValue().getEstimatedDeliveryDate().toString() : "Chưa có"));
        TableColumn<SiteOrderDTO, Void> actionCol = new TableColumn<>("Hành động");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("Xem chi tiết");
            {
                viewButton.getStyleClass().add("btn-outline");
                viewButton.setOnAction(e -> {
                    SiteOrderDTO item = getTableView().getItems().get(getIndex());
                    setPage(buildSiteOrderDetailPage(item));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });
        table.getColumns().addAll(idCol, siteCol, statusCol, etaCol, actionCol);
        return table;
    }

    private Node buildSiteInfoPage() {
        VBox page = pageShell("Quản lý thông tin site", "Xem thông tin site và danh sách sản phẩm tồn kho do site cung cấp.");

        TableView<Site> siteTable = buildSiteTable();

        try {
            List<Site> sites = siteRepository.findAll();
            siteTable.setItems(FXCollections.observableArrayList(sites));
        } catch (SQLException ex) {
            siteTable.setItems(FXCollections.observableArrayList());
            showError("Không tải được danh sách site", ex.getMessage());
        }

        page.getChildren().add(siteTable);
        return scroll(page);
    }

    private VBox buildSiteOrderCard(SiteOrderDTO order) {
        VBox card = new VBox(10);
        card.getStyleClass().add("detail-chip");
        card.setPadding(new Insets(14));

        Label title = new Label("Đơn #" + order.getId() + " - " + order.getSiteCode() + " - " + safe(order.getSiteName()));
        title.getStyleClass().add("detail-chip-title");

        HBox metaRow = new HBox(8);
        metaRow.getChildren().addAll(
                chip("Trạng thái: " + UiTexts.siteOrderStatus(order.getStatus()), "#ecfeff", "#0f766e"),
                chip("Hình thức: " + UiTexts.deliveryMeans(order.getDeliveryMeans()), "#eff6ff", "#2563eb"),
                chip("Ngày dự kiến: " + (order.getEstimatedDeliveryDate() != null ? order.getEstimatedDeliveryDate() : "Chưa có"), "#f8fafc", "#475569")
        );

        VBox items = new VBox(6);
        for (SiteOrderItemDTO item : order.getItems()) {
            VBox itemRow = new VBox(4);
            itemRow.getStyleClass().add("dialog-section-card");
            itemRow.setPadding(new Insets(10));
            Label itemTitle = new Label(item.getMerchandiseCode() + " - " + safe(item.getMerchandiseName()));
            itemTitle.setStyle("-fx-text-fill: #1e3a5f; -fx-font-size: 13px; -fx-font-weight: 700;");
            Label itemMeta = paragraph(item.getQuantity() + " " + safe(item.getUnit()));
            itemMeta.setStyle("-fx-text-fill: #4b6b9a; -fx-font-size: 13px;");
            itemRow.getChildren().addAll(itemTitle, itemMeta);
            items.getChildren().add(itemRow);
        }
        card.getChildren().addAll(title, metaRow, items);
        return card;
    }

    private Node buildSiteOrderDetailPage(SiteOrderDTO order) {
        VBox page = pageShell("Chi tiết đơn đặt hàng", "Xem thông tin chi tiết của đơn đặt hàng.");

        Button backButton = new Button("Quay lại danh sách");
        backButton.getStyleClass().add("btn-outline");
        backButton.setOnAction(e -> setPage(buildSiteOrderPage()));

        VBox detailBox = new VBox(14);
        detailBox.getChildren().addAll(
                buildDetailHeader(
                        "Đơn đặt hàng #" + order.getId(),
                        "Site",
                        order.getSiteCode() + " - " + safe(order.getSiteName()),
                        UiTexts.siteOrderStatus(order.getStatus())
                ),
                buildSiteOrderCard(order)
        );
        page.getChildren().addAll(backButton, detailBox);
        return scroll(page);
    }

    private String requestCreatorLabel(SaleRequestDTO request) {
        return request.getCreatedByUsername() != null && !request.getCreatedByUsername().isBlank()
                ? request.getCreatedByUsername()
                : "User #" + request.getCreatedBy();
    }

    private List<SaleRequestDTO> filterRequests(List<SaleRequestDTO> requests, SaleRequest.Status status) {
        return requests.stream()
                .filter(request -> request.getStatus() == status)
                .toList();
    }

    private boolean isReadyToConfirmSuccess(SaleRequestDTO request, List<SiteOrderDTO> orders) {
        if (orders.isEmpty()) {
            return false;
        }
        Map<Integer, Integer> confirmedByItem = new HashMap<>();
        for (SiteOrderDTO order : orders) {
            if (order.getStatus() != com.importorder.entity.SiteOrder.Status.WAREHOUSE_CONFIRMED
                    && order.getStatus() != com.importorder.entity.SiteOrder.Status.SHORTAGE_REPORTED) {
                continue;
            }
            for (SiteOrderItemDTO item : order.getItems()) {
                confirmedByItem.merge(
                        item.getSaleRequestItemId(),
                        item.getReceivedQuantity() != null ? item.getReceivedQuantity() : item.getQuantity(),
                        Integer::sum
                );
            }
        }
        for (SaleRequestItemDTO requestItem : request.getItems()) {
            if (confirmedByItem.getOrDefault(requestItem.getId(), 0) < requestItem.getQuantityOrdered()) {
                return false;
            }
        }
        return true;
    }

    private VBox buildProgressSummary(SaleRequestDTO request, List<SiteOrderDTO> orders) {
        Map<Integer, Integer> receivedByItem = new HashMap<>();
        for (SiteOrderDTO order : orders) {
            if (order.getStatus() != com.importorder.entity.SiteOrder.Status.WAREHOUSE_CONFIRMED
                    && order.getStatus() != com.importorder.entity.SiteOrder.Status.SHORTAGE_REPORTED) {
                continue;
            }
            for (SiteOrderItemDTO item : order.getItems()) {
                receivedByItem.merge(
                        item.getSaleRequestItemId(),
                        item.getReceivedQuantity() != null ? item.getReceivedQuantity() : item.getQuantity(),
                        Integer::sum
                );
            }
        }

        VBox wrapper = new VBox(12);
        wrapper.getStyleClass().add("progress-summary-card");

        Label title = new Label("Tổng hợp theo mặt hàng");
        title.getStyleClass().add("detail-title");

        VBox rows = new VBox(10);
        for (SaleRequestItemDTO requestItem : request.getItems()) {
            int received = receivedByItem.getOrDefault(requestItem.getId(), 0);
            int expected = requestItem.getQuantityOrdered();
            boolean complete = received >= expected;

            VBox row = new VBox(8);
            row.getStyleClass().add("progress-summary-row");

            HBox header = new HBox(10);
            header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label itemTitle = new Label(requestItem.getMerchandiseCode() + " - " + safe(requestItem.getMerchandiseName()));
            itemTitle.getStyleClass().add("progress-item-title");
            header.getChildren().addAll(
                    itemTitle,
                    chip("Đã nhận " + received + "/" + expected + " " + safe(requestItem.getUnit()),
                            complete ? "#dcfce7" : "#fef3c7",
                            complete ? "#15803d" : "#b45309")
            );

            double ratio = expected <= 0 ? 0 : Math.min(1.0, (double) received / expected);
            javafx.scene.control.ProgressBar progressBar = new javafx.scene.control.ProgressBar(ratio);
            progressBar.getStyleClass().add("progress-track");
            progressBar.setMaxWidth(Double.MAX_VALUE);

            // Label hint = new Label(
            //         complete
            //                 ? "Đã nhận đủ số lượng cho mặt hàng này."
            //                 : "Còn thiếu " + (expected - received) + " " + safe(requestItem.getUnit()) + " để có thể xác nhận hoàn thành."
            // );
            // hint.getStyleClass().add(complete ? "progress-ok-text" : "progress-pending-text");

            row.getChildren().addAll(header, progressBar);
            rows.getChildren().add(row);
        }

        wrapper.getChildren().addAll(title, rows);
        return wrapper;
    }

    private VBox buildProgressCard(SiteOrderDTO order) {
        VBox card = new VBox(14);
        card.getStyleClass().add("progress-order-card");

        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(order.getSiteCode() + " - " + safe(order.getSiteName()));
        title.getStyleClass().add("detail-chip-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label orderBadge = new Label("Đơn #" + order.getId());
        orderBadge.getStyleClass().add("progress-order-badge");
        header.getChildren().addAll(title, spacer, orderBadge);

        FlowPane meta = new FlowPane();
        meta.setHgap(8);
        meta.setVgap(8);
        meta.getChildren().addAll(
                chip("Trạng thái: " + UiTexts.siteOrderStatus(order.getStatus()), statusBackground(order), statusColor(order)),
                chip("Ngày dự kiến: " + (order.getEstimatedDeliveryDate() != null ? order.getEstimatedDeliveryDate() : "Chưa có"), "#eff6ff", "#2563eb"),
                chip("Ngày thực tế: " + (order.getActualDeliveryDate() != null ? order.getActualDeliveryDate() : "Chưa cập nhật"), "#f8fafc", "#475569")
        );

        VBox items = new VBox(10);
        for (SiteOrderItemDTO item : order.getItems()) {
            items.getChildren().add(buildProgressItemCard(item));
        }

        card.getChildren().addAll(header, meta, items);
        if (order.getStatus() == com.importorder.entity.SiteOrder.Status.CANCELLED
                && order.getCancelReason() != null && !order.getCancelReason().isBlank()) {
            card.getChildren().add(chip("Lý do hủy: " + order.getCancelReason(), "#fee2e2", "#b91c1c"));
        }
        return card;
    }

    private VBox buildProgressItemCard(SiteOrderItemDTO item) {
        VBox card = new VBox(8);
        card.getStyleClass().add("progress-item-card");

        Label title = new Label(item.getMerchandiseCode() + " - " + safe(item.getMerchandiseName()));
        title.getStyleClass().add("progress-item-title");

        int received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
        boolean hasReport = item.getReceivedQuantity() != null;
        boolean complete = hasReport && received >= item.getQuantity();

        FlowPane stats = new FlowPane();
        stats.setHgap(8);
        stats.setVgap(8);
        stats.getChildren().add(chip("Đặt: " + item.getQuantity() + " " + safe(item.getUnit()), "#eef2ff", "#4338ca"));
        stats.getChildren().add(chip(
                hasReport ? "Kho nhận: " + received + " " + safe(item.getUnit()) : "Kho chưa báo số lượng nhận",
                hasReport ? (complete ? "#dcfce7" : "#fef3c7") : "#f8fafc",
                hasReport ? (complete ? "#15803d" : "#b45309") : "#475569"
        ));
        if (hasReport && received < item.getQuantity()) {
            stats.getChildren().add(chip("Thiếu: " + (item.getQuantity() - received) + " " + safe(item.getUnit()), "#fee2e2", "#b91c1c"));
        }

        card.getChildren().addAll(title, stats);
        if (item.getShortageNote() != null && !item.getShortageNote().isBlank()) {
            Label note = paragraph("Ghi chú kho: " + item.getShortageNote());
            note.getStyleClass().add("progress-note-text");
            card.getChildren().add(note);
        }
        return card;
    }

    private String statusBackground(SiteOrderDTO order) {
        return switch (order.getStatus()) {
            case WAREHOUSE_CONFIRMED -> "#dcfce7";
            case SHORTAGE_REPORTED -> "#fef3c7";
            case CANCELLED -> "#fee2e2";
            default -> "#ecfeff";
        };
    }

    private String statusColor(SiteOrderDTO order) {
        return switch (order.getStatus()) {
            case WAREHOUSE_CONFIRMED -> "#15803d";
            case SHORTAGE_REPORTED -> "#b45309";
            case CANCELLED -> "#b91c1c";
            default -> "#0f766e";
        };
    }

    private void handleCancelRequest(SaleRequestDTO request) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Báo hủy yêu cầu");
        dialog.setHeaderText("Nhập lý do hủy để gửi lại cho bên sale");
        dialog.setContentText("Lý do:");
        Optional<String> reasonOpt = dialog.showAndWait();
        if (reasonOpt.isEmpty()) {
            return;
        }
        String reason = reasonOpt.get().trim();
        if (reason.isBlank()) {
            showError("Thiếu lý do", "Vui lòng nhập lý do hủy yêu cầu.");
            return;
        }
        try {
            overseasOrderController.cancelSaleRequestByOverseas(request.getId(), reason);
            planningStates.remove(request.getId());
            showInfo("Đã hủy yêu cầu", "Yêu cầu nhập hàng đã được chuyển sang trạng thái hủy.");
            refreshRequestDetailPage(reloadSaleRequest(request.getId()), false);
        } catch (Exception ex) {
            showError("Không thể hủy yêu cầu", ex.getMessage());
        }
    }

    private void handleProcessSaleCancellationRequest(SaleRequestDTO request) {
        try {
            overseasOrderController.processSaleCancellationRequest(request.getId());
            showInfo("Đã xử lý yêu cầu hủy", "Hệ thống đã cập nhật phản hồi mới nhất cho yêu cầu hủy từ bộ phận bán hàng.");
            refreshRequestDetailPage(reloadSaleRequest(request.getId()), false);
        } catch (Exception ex) {
            showError("Không thể xử lý yêu cầu hủy", ex.getMessage());
        }
    }

    private void handleCreateSiteOrders(SaleRequestDTO request, PlanningState state) {
        try {
            List<String> missing = validatePlanningCoverage(request, state);
            if (!missing.isEmpty()) {
                showError("Chưa đủ sản phẩm", "Vui lòng bổ sung đủ kế hoạch cho các mặt hàng còn thiếu:\n" + String.join("\n", missing));
                return;
            }

            List<SiteDraftSummary> grouped = new ArrayList<>(state.groupBySiteDraft().values());
            int createdCount = 0;
            for (SiteDraftSummary summary : grouped) {
                List<SiteOrderItemDTO> items = new ArrayList<>();
                for (PlannedAllocation allocation : summary.allocations) {
                    SiteOrderItemDTO itemDTO = new SiteOrderItemDTO();
                    itemDTO.setSaleRequestItemId(allocation.saleRequestItemId);
                    itemDTO.setMerchandiseCode(allocation.merchandiseCode);
                    itemDTO.setMerchandiseName(allocation.merchandiseName);
                    itemDTO.setQuantity(allocation.quantity);
                    itemDTO.setUnit(allocation.unit);
                    items.add(itemDTO);
                }
                overseasOrderController.createManualSiteOrder(
                        request.getId(),
                        summary.siteCode,
                        summary.deliveryMeans,
                        summary.arrivalDate,
                        items
                );
                createdCount++;
            }
            planningStates.remove(request.getId());
            showInfo("Tạo đơn thành công", "Đã tạo " + createdCount + " đơn đặt hàng theo site.");
            refreshRequestDetailPage(reloadSaleRequest(request.getId()), false);
        } catch (Exception ex) {
            showError("Không thể tạo đơn đặt hàng", ex.getMessage());
        }
    }

    private SaleRequestDTO reloadSaleRequest(int requestId) throws SQLException {
        return saleRequestController.getRequestDetail(requestId);
    }

    private void refreshRequestDetailPage(SaleRequestDTO request, boolean preserveScroll) {
        double scrollValue = preserveScroll ? currentScrollValue() : 0.0;
        Node page = buildRequestDetailPage(request);
        setPage(page);
        if (preserveScroll && page instanceof ScrollPane pane) {
            Platform.runLater(() -> pane.setVvalue(scrollValue));
        }
    }

    private double currentScrollValue() {
        Node current = contentArea.getChildren().isEmpty() ? null : contentArea.getChildren().get(0);
        if (current instanceof ScrollPane pane) {
            return pane.getVvalue();
        }
        return 0.0;
    }

    private List<String> validatePlanningCoverage(SaleRequestDTO request, PlanningState state) {
        List<String> missing = new ArrayList<>();
        for (SaleRequestItemDTO item : request.getItems()) {
            int allocated = state.totalAllocatedForItem(item.getId());
            if (allocated < item.getQuantityOrdered()) {
                missing.add(item.getMerchandiseCode() + " còn thiếu " + (item.getQuantityOrdered() - allocated) + " " + safe(item.getUnit()));
            }
        }
        return missing;
    }

    private TableView<Site> buildSiteTable() {
        TableView<Site> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(320);

        TableColumn<Site, String> codeCol = new TableColumn<>("Mã site");
        codeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCode()));
        TableColumn<Site, String> nameCol = new TableColumn<>("Tên site");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(safe(data.getValue().getName())));
        TableColumn<Site, String> shipCol = new TableColumn<>("Ngày tàu");
        shipCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getDeliveryDaysByShip())));
        TableColumn<Site, String> airCol = new TableColumn<>("Ngày hàng không");
        airCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getDeliveryDaysByAir())));
        TableColumn<Site, Void> actionCol = new TableColumn<>("Hành động");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("Xem chi tiết");
            {
                viewButton.getStyleClass().add("btn-outline");
                viewButton.setOnAction(e -> {
                    Site item = getTableView().getItems().get(getIndex());
                    setPage(buildSiteDetailPage(item));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });

        table.getColumns().addAll(codeCol, nameCol, shipCol, airCol, actionCol);
        return table;
    }

    private Node buildSiteDetailPage(Site site) {
        VBox page = pageShell("Chi tiết thông tin site", "Xem thời gian vận chuyển và tồn kho tham khảo của site.");

        Button backButton = new Button("Quay lại danh sách");
        backButton.getStyleClass().add("btn-outline");
        backButton.setOnAction(e -> setPage(buildSiteInfoPage()));

        VBox detailBox = new VBox(14);
        detailBox.getChildren().addAll(
                buildDetailHeader(
                        site.getCode() + " - " + site.getName(),
                        "Ghi chú",
                        safe(site.getNote()),
                        "Site hoạt động"
                ),
                buildSiteSummaryCard(site),
                sectionTitle("Danh sách sản phẩm tại site")
        );

        VBox merchBox = new VBox(8);
        try {
            List<SiteMerchandise> merchandises = siteMerchandiseRepository.findBySiteCode(site.getCode());
            if (merchandises.isEmpty()) {
                merchBox.getChildren().add(buildEmptyState("Site này chưa có dữ liệu sản phẩm."));
            } else {
                for (SiteMerchandise merch : merchandises) {
                    merchBox.getChildren().add(buildSiteMerchandiseCard(merch));
                }
            }
        } catch (SQLException ex) {
            merchBox.getChildren().add(buildEmptyState("Không tải được sản phẩm của site: " + ex.getMessage()));
        }

        detailBox.getChildren().add(merchBox);
        page.getChildren().addAll(backButton, detailBox);
        return scroll(page);
    }

    private VBox buildDetailHeader(String titleText, String metaLabel, String metaValue, String statusText) {
        VBox header = new VBox(10);
        header.getStyleClass().add("card");
        header.setPadding(new Insets(18));

        Label title = new Label(titleText);
        title.getStyleClass().add("detail-title");

        HBox metaRow = new HBox(10);
        metaRow.setFillHeight(true);
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
            VBox row = new VBox(6);
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

    private VBox buildSiteSummaryCard(Site site) {
        VBox card = new VBox(10);
        card.getStyleClass().add("detail-chip");
        card.setPadding(new Insets(14));

        HBox metaRow = new HBox(8);
        metaRow.getChildren().addAll(
                chip("Vận chuyển tàu: " + site.getDeliveryDaysByShip() + " ngày", "#eff6ff", "#2563eb"),
                chip("Vận chuyển hàng không: " + site.getDeliveryDaysByAir() + " ngày", "#f0fdf4", "#16a34a")
        );
        card.getChildren().add(metaRow);
        return card;
    }

    private VBox buildSiteMerchandiseCard(SiteMerchandise merch) {
        VBox row = new VBox(4);
        row.getStyleClass().add("detail-chip");
        row.setPadding(new Insets(12));
        Label title = new Label(merch.getMerchandiseCode());
        title.getStyleClass().add("detail-chip-title");
        HBox metaRow = new HBox(8);
        metaRow.getChildren().addAll(
                chip("Số lượng tồn kho tham khảo: " + merch.getQuantity(), "#eff6ff", "#2563eb"),
                chip("Đơn vị: " + safe(merch.getUnit()), "#f8fafc", "#475569")
        );
        row.getChildren().addAll(title, metaRow);
        return row;
    }

    private VBox chipBlock(String text, String background, String color) {
        VBox box = new VBox();
        box.getChildren().add(chip(text, background, color));
        return box;
    }

    private VBox field(String label, Node node) {
        VBox box = new VBox(6);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        box.getChildren().addAll(lbl, node);
        return box;
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

    private static final class PlanningState {
        private final List<PlannedAllocation> allocations = new ArrayList<>();
        private final Set<Integer> expandedItemIds = new HashSet<>();
        private boolean plannerVisible;

        private void replaceWithSuggestion(OverseasOrderSuggestionEngine.SuggestionResult suggestion) {
            allocations.clear();
            for (OverseasOrderSuggestionEngine.SuggestedAllocation allocation : suggestion.allocations()) {
                allocations.add(new PlannedAllocation(
                        allocation.saleRequestItemId(),
                        allocation.merchandiseCode(),
                        allocation.merchandiseName(),
                        allocation.siteCode(),
                        allocation.siteName(),
                        allocation.deliveryMeans(),
                        allocation.arrivalDate(),
                        allocation.quantity(),
                        allocation.unit()
                ));
            }
            plannerVisible = true;
        }

        private void addAllocation(PlannedAllocation allocation) {
            allocations.add(allocation);
            plannerVisible = true;
        }

        private void removeAllocation(PlannedAllocation allocation) {
            allocations.remove(allocation);
        }

        private int totalAllocatedForItem(int saleRequestItemId) {
            return allocations.stream()
                    .filter(allocation -> allocation.saleRequestItemId == saleRequestItemId)
                    .mapToInt(allocation -> allocation.quantity)
                    .sum();
        }

        private void toggleItemExpansion(int saleRequestItemId) {
            if (!expandedItemIds.add(saleRequestItemId)) {
                expandedItemIds.remove(saleRequestItemId);
            }
        }

        private Map<String, SiteDraftSummary> groupBySiteDraft() {
            Map<String, SiteDraftSummary> grouped = new LinkedHashMap<>();
            for (PlannedAllocation allocation : allocations) {
                String key = allocation.siteCode + "|" + allocation.deliveryMeans + "|" + allocation.arrivalDate;
                SiteDraftSummary summary = grouped.computeIfAbsent(key, ignored ->
                        new SiteDraftSummary(
                                allocation.siteCode,
                                allocation.siteName,
                                allocation.deliveryMeans,
                                allocation.arrivalDate,
                                new ArrayList<>()
                        ));
                summary.allocations.add(allocation);
            }
            return grouped;
        }
    }

    private static final class PlannedAllocation {
        private final int saleRequestItemId;
        private final String merchandiseCode;
        private final String merchandiseName;
        private final String siteCode;
        private final String siteName;
        private final com.importorder.entity.SiteOrder.DeliveryMeans deliveryMeans;
        private final LocalDate arrivalDate;
        private final int quantity;
        private final String unit;

        private PlannedAllocation(int saleRequestItemId,
                                  String merchandiseCode,
                                  String merchandiseName,
                                  String siteCode,
                                  String siteName,
                                  com.importorder.entity.SiteOrder.DeliveryMeans deliveryMeans,
                                  LocalDate arrivalDate,
                                  int quantity,
                                  String unit) {
            this.saleRequestItemId = saleRequestItemId;
            this.merchandiseCode = merchandiseCode;
            this.merchandiseName = merchandiseName;
            this.siteCode = siteCode;
            this.siteName = siteName;
            this.deliveryMeans = deliveryMeans;
            this.arrivalDate = arrivalDate;
            this.quantity = quantity;
            this.unit = unit;
        }
    }

    private static final class SiteDraftSummary {
        private final String siteCode;
        private final String siteName;
        private final com.importorder.entity.SiteOrder.DeliveryMeans deliveryMeans;
        private final LocalDate arrivalDate;
        private final List<PlannedAllocation> allocations;

        private SiteDraftSummary(String siteCode,
                                 String siteName,
                                 com.importorder.entity.SiteOrder.DeliveryMeans deliveryMeans,
                                 LocalDate arrivalDate,
                                 List<PlannedAllocation> allocations) {
            this.siteCode = siteCode;
            this.siteName = siteName;
            this.deliveryMeans = deliveryMeans;
            this.arrivalDate = arrivalDate;
            this.allocations = allocations;
        }
    }
}
