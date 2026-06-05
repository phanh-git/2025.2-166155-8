package com.importorder.service;

import com.importorder.config.DatabaseConnection;
import com.importorder.dto.*;
import com.importorder.entity.*;
import com.importorder.repository.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service cốt lõi của Bộ phận đặt hàng quốc tế.
 *
 * Thuật toán chọn site (xử lý ĐỘC LẬP từng mặt hàng):
 *   1. Lọc các site đáp ứng được ngày nhận mong muốn
 *   2. Ưu tiên phương tiện tàu hơn hàng không
 *   3. Trong cùng phương tiện, ưu tiên site có tồn kho lớn
 *   4. Dùng ít site nhất có thể
 *   5. Nếu không đủ hàng → ghi lỗi
 */
public class OverseasOrderService {

    private final SaleRequestRepository saleRequestRepo;
    private final SaleRequestItemRepository saleRequestItemRepo;
    private final SiteRepository siteRepo;
    private final SiteMerchandiseRepository siteMerchandiseRepo;
    private final SiteOrderRepository siteOrderRepo;
    private final SiteOrderItemRepository siteOrderItemRepo;
    private final MerchandiseRepository merchandiseRepo;

    public OverseasOrderService() {
        this(new SaleRequestRepository(),
                new SaleRequestItemRepository(),
                new SiteRepository(),
                new SiteMerchandiseRepository(),
                new SiteOrderRepository(),
                new SiteOrderItemRepository(),
                new MerchandiseRepository());
    }

    OverseasOrderService(SaleRequestRepository saleRequestRepo,
                         SaleRequestItemRepository saleRequestItemRepo,
                         SiteRepository siteRepo,
                         SiteMerchandiseRepository siteMerchandiseRepo,
                         SiteOrderRepository siteOrderRepo,
                         SiteOrderItemRepository siteOrderItemRepo,
                         MerchandiseRepository merchandiseRepo) {
        this.saleRequestRepo = saleRequestRepo;
        this.saleRequestItemRepo = saleRequestItemRepo;
        this.siteRepo = siteRepo;
        this.siteMerchandiseRepo = siteMerchandiseRepo;
        this.siteOrderRepo = siteOrderRepo;
        this.siteOrderItemRepo = siteOrderItemRepo;
        this.merchandiseRepo = merchandiseRepo;
    }

    // ─── PROCESS SALE REQUEST ────────────────────────────────────────────────────

    /**
     * Xử lý toàn bộ một SaleRequest: chạy thuật toán chọn site cho từng mặt hàng,
     * tạo các SiteOrder tương ứng.
     *
     * @param saleRequestId     id của sale_request cần xử lý
     * @param processedByUserId id của nhân viên đặt hàng quốc tế đang xử lý
     * @return                  ProcessResult chứa đơn đã tạo + danh sách lỗi
     */
    public ProcessResult processSaleRequest(int saleRequestId,
                                            int processedByUserId) throws SQLException {

        // 1. Load và kiểm tra trạng thái
        SaleRequest request = saleRequestRepo.findById(saleRequestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy sale request id=" + saleRequestId));

        if (request.getStatus() != SaleRequest.Status.RECEIVED) {
            throw new IllegalStateException(
                    "Sale request đang ở trạng thái [" + request.getStatus()
                    + "], không thể xử lý lại");
        }

        // 2. Đánh dấu đang xử lý
        saleRequestRepo.updateStatus(saleRequestId, SaleRequest.Status.IN_PROGRESS);

        ProcessResult result = new ProcessResult();

        // 3. Load danh sách items
        List<SaleRequestItem> items = saleRequestItemRepo.findBySaleRequestId(saleRequestId);

        // 4. Load toàn bộ thông tin site một lần (tránh query lặp)
        List<Site> allSites = siteRepo.findAll();
        Map<String, Site> siteMap = new HashMap<>();
        for (Site s : allSites) siteMap.put(s.getCode(), s);

        // 5. Xử lý từng mặt hàng độc lập
        // Gom kết quả phân công: siteCode -> (saleRequestItemId -> quantity)
        // Mỗi key là một SiteOrder tiềm năng
        Map<String, List<SiteOrderItem>> siteToItems = new LinkedHashMap<>();
        Map<String, SiteOrder.DeliveryMeans> siteDeliveryMeans = new HashMap<>();

        for (SaleRequestItem item : items) {
            processItem(item, siteMap, siteToItems, siteDeliveryMeans, result);
        }

        // 6. Tạo SiteOrder cho từng site được chọn
        for (Map.Entry<String, List<SiteOrderItem>> entry : siteToItems.entrySet()) {
            String siteCode = entry.getKey();
            List<SiteOrderItem> orderItems = entry.getValue();
            SiteOrder.DeliveryMeans means = siteDeliveryMeans.get(siteCode);
            Site site = siteMap.get(siteCode);

            int deliveryDays = (means == SiteOrder.DeliveryMeans.SHIP_DELIVERY)
                    ? site.getDeliveryDaysByShip()
                    : site.getDeliveryDaysByAir();

            SiteOrder order = new SiteOrder();
            order.setSaleRequestId(saleRequestId);
            order.setSiteCode(siteCode);
            order.setStatus(SiteOrder.Status.ORDER_RECEIVED);
            order.setDeliveryMeans(means);
            order.setEstimatedDeliveryDate(LocalDate.now().plusDays(deliveryDays));
            order.setCreatedAt(LocalDateTime.now());
            order.setCreatedBy(processedByUserId);

            // insertWithItems xử lý transaction: insert order + items + giảm tồn kho
            int orderId = siteOrderRepo.insertWithItems(order, orderItems);
            order.setId(orderId);

            // Build DTO để trả về
            SiteOrderDTO orderDTO = buildSiteOrderDTO(order, orderItems);
            result.addOrder(orderDTO);
        }

        // 7. Cập nhật trạng thái cuối
        saleRequestRepo.updateStatus(saleRequestId, SaleRequest.Status.IN_PROGRESS);
        result.setFullyProcessed(!result.hasErrors());

        return result;
    }

    // ─── THUẬT TOÁN CHỌN SITE ────────────────────────────────────────────────────

    /**
     * Xử lý một mặt hàng: tìm site phù hợp và ghi kết quả phân công vào siteToItems.
     */
    private void processItem(SaleRequestItem item,
                             Map<String, Site> siteMap,
                             Map<String, List<SiteOrderItem>> siteToItems,
                             Map<String, SiteOrder.DeliveryMeans> siteDeliveryMeans,
                             ProcessResult result) throws SQLException {

        int remaining = item.getQuantityOrdered();
        LocalDate desiredDate = item.getDesiredDeliveryDate();
        String merchandiseCode = item.getMerchandiseCode();

        // Lấy tất cả site có hàng cho mặt hàng này, sort theo quantity DESC (đã xử lý trong repo)
        List<SiteMerchandise> available = siteMerchandiseRepo
                .findAvailableSitesForMerchandise(merchandiseCode);

        if (available.isEmpty()) {
            result.addError("Không có site nào cung cấp mặt hàng: " + merchandiseCode);
            return;
        }

        // Phân loại site theo phương tiện có thể đáp ứng ngày nhận
        List<SiteMerchandise> byShip = new ArrayList<>();
        List<SiteMerchandise> byAir  = new ArrayList<>();

        for (SiteMerchandise sm : available) {
            Site site = siteMap.get(sm.getSiteCode());
            if (site == null) continue;

            LocalDate arrivalByShip = LocalDate.now().plusDays(site.getDeliveryDaysByShip());
            LocalDate arrivalByAir  = LocalDate.now().plusDays(site.getDeliveryDaysByAir());

            if (!arrivalByShip.isAfter(desiredDate)) byShip.add(sm);
            else if (!arrivalByAir.isAfter(desiredDate)) byAir.add(sm);
            // Nếu cả hai đều không kịp → bỏ qua site này
        }

        // Ưu tiên tàu trước, nếu không đủ thì dùng thêm hàng không
        // byShip đã sort quantity DESC (từ repo), byAir cũng vậy
        remaining = allocate(item, byShip,  SiteOrder.DeliveryMeans.SHIP_DELIVERY,
                             siteToItems, siteDeliveryMeans, remaining);
        remaining = allocate(item, byAir,   SiteOrder.DeliveryMeans.AIR_DELIVERY,
                             siteToItems, siteDeliveryMeans, remaining);

        if (remaining > 0) {
            result.addError(
                    "Không đủ hàng cho mặt hàng [" + merchandiseCode + "]: "
                    + "cần thêm " + remaining + " " + item.getUnit());
        }
    }

    /**
     * Phân bổ số lượng còn thiếu từ danh sách site theo một phương tiện vận chuyển.
     * Ưu tiên site có tồn kho lớn (list đã sort DESC), dùng ít site nhất có thể.
     *
     * @return số lượng vẫn còn thiếu sau khi phân bổ
     */
    private int allocate(SaleRequestItem item,
                         List<SiteMerchandise> candidates,
                         SiteOrder.DeliveryMeans means,
                         Map<String, List<SiteOrderItem>> siteToItems,
                         Map<String, SiteOrder.DeliveryMeans> siteDeliveryMeans,
                         int remaining) {

        for (SiteMerchandise sm : candidates) {
            if (remaining <= 0) break;

            int take = Math.min(sm.getQuantity(), remaining);
            if (take <= 0) continue;

            String siteCode = sm.getSiteCode();

            // Ghi nhận phương tiện cho site này (nếu chưa có)
            siteDeliveryMeans.putIfAbsent(siteCode, means);

            // Tạo SiteOrderItem
            SiteOrderItem orderItem = new SiteOrderItem();
            orderItem.setSaleRequestItemId(item.getId());
            orderItem.setMerchandiseCode(item.getMerchandiseCode());
            orderItem.setQuantity(take);
            orderItem.setUnit(item.getUnit());

            siteToItems.computeIfAbsent(siteCode, k -> new ArrayList<>()).add(orderItem);

            remaining -= take;
        }

        return remaining;
    }

    // ─── GET SITE ORDERS ─────────────────────────────────────────────────────────

    /**
     * Lấy danh sách site_order thuộc một sale_request (để hiển thị kết quả xử lý).
     */
    public List<SiteOrderDTO> getSiteOrdersBySaleRequest(int saleRequestId) throws SQLException {
        List<SiteOrder> orders = siteOrderRepo.findBySaleRequestId(saleRequestId);
        List<SiteOrderDTO> result = new ArrayList<>();
        for (SiteOrder o : orders) {
            List<SiteOrderItem> items = siteOrderItemRepo.findBySiteOrderId(o.getId());
            result.add(buildSiteOrderDTO(o, items));
        }
        return result;
    }

    /**
     * Lấy danh sách site_order của một site cụ thể (dành cho site user xem đơn của mình).
     */
    public List<SiteOrderDTO> getSiteOrdersBySite(String siteCode) throws SQLException {
        List<SiteOrder> orders = siteOrderRepo.findBySiteCode(siteCode);
        List<SiteOrderDTO> result = new ArrayList<>();
        for (SiteOrder o : orders) {
            List<SiteOrderItem> items = siteOrderItemRepo.findBySiteOrderId(o.getId());
            result.add(buildSiteOrderDTO(o, items));
        }
        return result;
    }

    public List<SiteOrderDTO> getAllSiteOrders() throws SQLException {
        List<SiteOrder> orders = siteOrderRepo.findAll();
        List<SiteOrderDTO> result = new ArrayList<>();
        for (SiteOrder order : orders) {
            List<SiteOrderItem> items = siteOrderItemRepo.findBySiteOrderId(order.getId());
            result.add(buildSiteOrderDTO(order, items));
        }
        return result;
    }

    public SiteOrderDTO createManualSiteOrder(int saleRequestId,
                                              String siteCode,
                                              SiteOrder.DeliveryMeans deliveryMeans,
                                              LocalDate estimatedDeliveryDate,
                                              List<SiteOrderItemDTO> itemInputs,
                                              int createdByUserId) throws SQLException {
        SaleRequest request = saleRequestRepo.findById(saleRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau nhap hang #" + saleRequestId));
        siteRepo.findByCode(siteCode)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay site [" + siteCode + "]"));

        if (request.getStatus() == SaleRequest.Status.CANCELLED) {
            throw new IllegalStateException("Yeu cau nhap hang da bi huy");
        }
        if (itemInputs == null || itemInputs.isEmpty()) {
            throw new IllegalArgumentException("Phai chon it nhat mot mat hang cho don dat hang");
        }

        List<SiteOrderItem> orderItems = new ArrayList<>();
        for (SiteOrderItemDTO itemInput : itemInputs) {
            SaleRequestItem saleRequestItem = saleRequestItemRepo.findById(itemInput.getSaleRequestItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Khong tim thay dong hang yeu cau #" + itemInput.getSaleRequestItemId()));
            if (saleRequestItem.getSaleRequestId() != saleRequestId) {
                throw new IllegalArgumentException("Mat hang khong thuoc yeu cau nhap hang dang chon");
            }
            if (itemInput.getQuantity() <= 0) {
                throw new IllegalArgumentException("So luong dat hang phai lon hon 0");
            }
            SiteOrderItem orderItem = new SiteOrderItem();
            orderItem.setSaleRequestItemId(saleRequestItem.getId());
            orderItem.setMerchandiseCode(saleRequestItem.getMerchandiseCode());
            orderItem.setQuantity(itemInput.getQuantity());
            orderItem.setUnit(itemInput.getUnit() != null && !itemInput.getUnit().isBlank()
                    ? itemInput.getUnit()
                    : saleRequestItem.getUnit());
            orderItems.add(orderItem);
        }

        SiteOrder order = new SiteOrder();
        order.setSaleRequestId(saleRequestId);
        order.setSiteCode(siteCode);
        order.setStatus(SiteOrder.Status.ORDER_RECEIVED);
        order.setDeliveryMeans(deliveryMeans);
        order.setEstimatedDeliveryDate(estimatedDeliveryDate);
        order.setCreatedAt(LocalDateTime.now());
        order.setCreatedBy(createdByUserId);

        int orderId = siteOrderRepo.insertWithItems(order, orderItems);
        order.setId(orderId);

        if (request.getStatus() == SaleRequest.Status.RECEIVED) {
            saleRequestRepo.updateStatus(saleRequestId, SaleRequest.Status.IN_PROGRESS);
        }

        return buildSiteOrderDTO(order, orderItems);
    }

    public void processSaleCancellationRequest(int saleRequestId) throws SQLException {
        SaleRequest request = saleRequestRepo.findById(saleRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sale request id=" + saleRequestId));
        if (!request.isCancelRequested()) {
            throw new IllegalStateException("Yêu cầu này chưa có đề nghị hủy từ bộ phận bán hàng");
        }
        if (request.getStatus() == SaleRequest.Status.SUCCESS) {
            throw new IllegalStateException("Không thể hủy yêu cầu đã hoàn thành");
        }
        if (request.getStatus() == SaleRequest.Status.CANCELLED) {
            throw new IllegalStateException("Yêu cầu này đã bị hủy");
        }

        List<SiteOrder> orders = siteOrderRepo.findBySaleRequestId(saleRequestId);
        if (orders.isEmpty()) {
            saleRequestRepo.updateStatusAndCancellation(
                    saleRequestId,
                    SaleRequest.Status.CANCELLED,
                    false,
                    request.getCancelRequestReason(),
                    "Đã hủy theo yêu cầu từ bộ phận bán hàng trước khi tạo đơn đặt hàng tới site."
            );
            return;
        }

        boolean hasInTransitOrBeyond = orders.stream().anyMatch(order ->
                order.getStatus() == SiteOrder.Status.IN_TRANSIT
                        || order.getStatus() == SiteOrder.Status.DELIVERED
                        || order.getStatus() == SiteOrder.Status.WAREHOUSE_CONFIRMED);
        if (hasInTransitOrBeyond) {
            saleRequestRepo.resolveCancellationRequest(
                    saleRequestId,
                    false,
                    "Không thể hủy vì đã có đơn hàng đang vận chuyển hoặc đã giao tới kho."
            );
            return;
        }

        for (SiteOrder order : orders) {
            if (order.getStatus() != SiteOrder.Status.CANCELLED) {
                siteOrderRepo.updateStatusWithReason(
                        order.getId(),
                        SiteOrder.Status.CANCELLED,
                        "Đã hủy theo yêu cầu hủy từ bộ phận bán hàng."
                );
            }
        }
        saleRequestRepo.updateStatusAndCancellation(
                saleRequestId,
                SaleRequest.Status.CANCELLED,
                false,
                request.getCancelRequestReason(),
                "Đã hủy theo yêu cầu từ bộ phận bán hàng và đã thông báo hủy tới các site liên quan."
        );
    }

    public void cancelSaleRequestByOverseas(int saleRequestId, String reason) throws SQLException {
        SaleRequest request = saleRequestRepo.findById(saleRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sale request id=" + saleRequestId));
        if (request.getStatus() == SaleRequest.Status.SUCCESS) {
            throw new IllegalStateException("Không thể hủy yêu cầu đã hoàn thành");
        }
        if (request.getStatus() == SaleRequest.Status.CANCELLED) {
            throw new IllegalStateException("Yêu cầu này đã bị hủy");
        }

        List<SiteOrder> orders = siteOrderRepo.findBySaleRequestId(saleRequestId);
        boolean hasInTransitOrBeyond = orders.stream().anyMatch(order ->
                order.getStatus() == SiteOrder.Status.IN_TRANSIT
                        || order.getStatus() == SiteOrder.Status.DELIVERED
                        || order.getStatus() == SiteOrder.Status.WAREHOUSE_CONFIRMED);
        if (hasInTransitOrBeyond) {
            throw new IllegalStateException("Không thể hủy vì đã có đơn hàng đang vận chuyển hoặc đã giao tới kho");
        }

        for (SiteOrder order : orders) {
            if (order.getStatus() != SiteOrder.Status.CANCELLED) {
                siteOrderRepo.updateStatusWithReason(order.getId(), SiteOrder.Status.CANCELLED, reason);
            }
        }
        saleRequestRepo.updateStatusAndCancellation(
                saleRequestId,
                SaleRequest.Status.CANCELLED,
                false,
                request.getCancelRequestReason(),
                reason
        );
    }

    // ─── UPDATE STATUS ───────────────────────────────────────────────────────────

    /**
     * Site cập nhật trạng thái đơn hàng (ví dụ: IN_TRANSIT, DELIVERED).
     */
    public void updateSiteOrderStatus(int siteOrderId,
                                      SiteOrder.Status newStatus,
                                      String siteCode) throws SQLException {
        SiteOrder order = siteOrderRepo.findById(siteOrderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy site_order id=" + siteOrderId));

        // Site chỉ được cập nhật đơn của chính mình
        if (!order.getSiteCode().equals(siteCode)) {
            throw new IllegalStateException("Bạn không có quyền cập nhật đơn hàng này");
        }

        siteOrderRepo.updateStatus(siteOrderId, newStatus);
        recalculateSaleRequestStatus(order.getSaleRequestId());
    }

    public void cancelSiteOrderBySite(int siteOrderId, String siteCode, String reason) throws SQLException {
        SiteOrder order = siteOrderRepo.findById(siteOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy site_order id=" + siteOrderId));
        if (!order.getSiteCode().equals(siteCode)) {
            throw new IllegalStateException("Bạn không có quyền cập nhật đơn hàng này");
        }
        if (order.getStatus() == SiteOrder.Status.IN_TRANSIT
                || order.getStatus() == SiteOrder.Status.DELIVERED
                || order.getStatus() == SiteOrder.Status.WAREHOUSE_CONFIRMED) {
            throw new IllegalStateException("Không thể hủy đơn đã bắt đầu vận chuyển hoặc đã về kho");
        }
        if (order.getStatus() == SiteOrder.Status.CANCELLED) {
            throw new IllegalStateException("Đơn hàng này đã bị hủy");
        }
        siteOrderRepo.updateStatusWithReason(
                siteOrderId,
                SiteOrder.Status.CANCELLED,
                reason == null || reason.isBlank() ? "Site từ chối xử lý đơn hàng." : reason.trim()
        );
        recalculateSaleRequestStatus(order.getSaleRequestId());
    }

    /**
     * Quản lý kho xác nhận hàng về kho.
     */
    public void confirmWarehouseReceipt(int siteOrderId,
                                        LocalDate actualDeliveryDate,
                                        List<SiteOrderItemDTO> reportedItems) throws SQLException {
        SiteOrder order = siteOrderRepo.findById(siteOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don dat hang #" + siteOrderId));
        List<SiteOrderItem> persistedItems = siteOrderItemRepo.findBySiteOrderId(siteOrderId);
        if (persistedItems.isEmpty()) {
            throw new IllegalStateException("Đơn hàng không có mặt hàng nào để xác nhận");
        }

        Map<Integer, SiteOrderItemDTO> reportByItemId = new HashMap<>();
        if (reportedItems != null) {
            for (SiteOrderItemDTO reportedItem : reportedItems) {
                reportByItemId.put(reportedItem.getId(), reportedItem);
            }
        }

        boolean hasShortage = false;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (SiteOrderItem persistedItem : persistedItems) {
                    SiteOrderItemDTO report = reportByItemId.get(persistedItem.getId());
                    int receivedQuantity = report != null && report.getReceivedQuantity() != null
                            ? report.getReceivedQuantity()
                            : persistedItem.getQuantity();
                    if (receivedQuantity < 0 || receivedQuantity > persistedItem.getQuantity()) {
                        throw new IllegalArgumentException("Số lượng thực nhận không hợp lệ cho mặt hàng " + persistedItem.getMerchandiseCode());
                    }

                    String shortageNote = report != null ? report.getShortageNote() : null;
                    persistedItem.setReceivedQuantity(receivedQuantity);
                    if (receivedQuantity < persistedItem.getQuantity()) {
                        hasShortage = true;
                        persistedItem.setShortageNote(shortageNote == null || shortageNote.isBlank()
                                ? "Kho báo thiếu " + (persistedItem.getQuantity() - receivedQuantity) + " " + persistedItem.getUnit()
                                : shortageNote.trim());
                    } else {
                        persistedItem.setShortageNote(null);
                    }
                    siteOrderItemRepo.updateWarehouseReport(conn, persistedItem);
                }

                siteOrderRepo.updateWarehouseStatus(
                        conn,
                        siteOrderId,
                        hasShortage ? SiteOrder.Status.SHORTAGE_REPORTED : SiteOrder.Status.WAREHOUSE_CONFIRMED,
                        actualDeliveryDate
                );
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        recalculateSaleRequestStatus(order.getSaleRequestId());
    }

    public void confirmSaleRequestSuccess(int saleRequestId) throws SQLException {
        SaleRequest request = saleRequestRepo.findById(saleRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sale request id=" + saleRequestId));

        if (request.getStatus() == SaleRequest.Status.CANCELLED) {
            throw new IllegalStateException("Không thể xác nhận yêu cầu đã bị hủy");
        }
        if (request.getStatus() == SaleRequest.Status.SUCCESS) {
            return;
        }
        if (!isSaleRequestFullyConfirmed(saleRequestId)) {
            throw new IllegalStateException("Chưa thể xác nhận thành công vì tổng số lượng kho đã nhận vẫn chưa đáp ứng đủ yêu cầu");
        }

        saleRequestRepo.updateStatus(saleRequestId, SaleRequest.Status.SUCCESS);
    }

    private void recalculateSaleRequestStatus(int saleRequestId) throws SQLException {
        SaleRequest request = saleRequestRepo.findById(saleRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sale request id=" + saleRequestId));

        if (request.getStatus() == SaleRequest.Status.CANCELLED
                || request.getStatus() == SaleRequest.Status.SUCCESS) {
            return;
        }

        if (!hasAnySiteOrder(saleRequestId)) {
            saleRequestRepo.updateStatus(saleRequestId, SaleRequest.Status.RECEIVED);
            return;
        }

        saleRequestRepo.updateStatus(saleRequestId, SaleRequest.Status.IN_PROGRESS);
    }

    public boolean isSaleRequestFullyConfirmed(int saleRequestId) throws SQLException {
        List<SaleRequestItem> requestItems = saleRequestItemRepo.findBySaleRequestId(saleRequestId);
        List<SiteOrder> orders = siteOrderRepo.findBySaleRequestId(saleRequestId);

        if (orders.isEmpty()) {
            return false;
        }

        Map<Integer, Integer> confirmedQuantityByItem = new HashMap<>();
        for (SiteOrder order : orders) {
            if (order.getStatus() != SiteOrder.Status.WAREHOUSE_CONFIRMED
                    && order.getStatus() != SiteOrder.Status.SHORTAGE_REPORTED) {
                continue;
            }
            List<SiteOrderItem> orderItems = siteOrderItemRepo.findBySiteOrderId(order.getId());
            for (SiteOrderItem orderItem : orderItems) {
                confirmedQuantityByItem.merge(
                        orderItem.getSaleRequestItemId(),
                        orderItem.getReceivedQuantity() != null ? orderItem.getReceivedQuantity() : orderItem.getQuantity(),
                        Integer::sum
                );
            }
        }

        boolean fullyConfirmed = true;
        for (SaleRequestItem requestItem : requestItems) {
            int confirmed = confirmedQuantityByItem.getOrDefault(requestItem.getId(), 0);
            if (confirmed < requestItem.getQuantityOrdered()) {
                return false;
            }
        }

        return fullyConfirmed;
    }

    private boolean hasAnySiteOrder(int saleRequestId) throws SQLException {
        return !siteOrderRepo.findBySaleRequestId(saleRequestId).isEmpty();
    }

    // ─── MAPPER ──────────────────────────────────────────────────────────────────

    private SiteOrderDTO buildSiteOrderDTO(SiteOrder order,
                                           List<SiteOrderItem> items) throws SQLException {
        SiteOrderDTO dto = new SiteOrderDTO();
        dto.setId(order.getId());
        dto.setSaleRequestId(order.getSaleRequestId());
        dto.setSiteCode(order.getSiteCode());
        dto.setStatus(order.getStatus());
        dto.setDeliveryMeans(order.getDeliveryMeans());
        dto.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());
        dto.setActualDeliveryDate(order.getActualDeliveryDate());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setCreatedBy(order.getCreatedBy());
        dto.setCancelReason(order.getCancelReason());

        // Enrich tên site
        siteRepo.findByCode(order.getSiteCode())
                .ifPresent(s -> dto.setSiteName(s.getName()));

        // Build item DTOs
        List<SiteOrderItemDTO> itemDTOs = new ArrayList<>();
        for (SiteOrderItem item : items) {
            SiteOrderItemDTO itemDTO = new SiteOrderItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setSiteOrderId(item.getSiteOrderId());
            itemDTO.setSaleRequestItemId(item.getSaleRequestItemId());
            itemDTO.setMerchandiseCode(item.getMerchandiseCode());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setUnit(item.getUnit());
            itemDTO.setReceivedQuantity(item.getReceivedQuantity());
            itemDTO.setShortageNote(item.getShortageNote());
            merchandiseRepo.findByCode(item.getMerchandiseCode())
                    .ifPresent(m -> itemDTO.setMerchandiseName(m.getName()));
            itemDTOs.add(itemDTO);
        }
        dto.setItems(itemDTOs);
        return dto;
    }
}
