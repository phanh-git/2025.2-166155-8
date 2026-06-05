package com.importorder.controller;

import com.importorder.dto.ProcessResult;
import com.importorder.dto.InventoryUpdateRequestDTO;
import com.importorder.dto.SiteOrderDTO;
import com.importorder.dto.SiteOrderItemDTO;
import com.importorder.dto.SaleRequestDTO;
import com.importorder.entity.SiteOrder;
import com.importorder.entity.SiteMerchandise;
import com.importorder.entity.User;
import com.importorder.service.SaleRequestService;
import com.importorder.service.OverseasOrderService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller cho Bộ phận đặt hàng quốc tế và Site user.
 *
 * Bộ phận đặt hàng quốc tế:
 *   - Xử lý sale request (chạy thuật toán chọn site)
 *   - Xem danh sách site_order theo sale_request
 *
 * Site user:
 *   - Xem đơn hàng của site mình
 *   - Cập nhật trạng thái vận chuyển
 *
 * Quản lý kho:
 *   - Xác nhận hàng về kho
 */
public class OverseasOrderController {

    private final OverseasOrderService overseasOrderService = new OverseasOrderService();
    private final SaleRequestService saleRequestService = new SaleRequestService();

    /** Được inject từ LoginController */
    private User currentUser;

    /** Được inject khi currentUser là SITE_USER (lấy từ SiteUserRepository) */
    private String currentSiteCode;

    // ─── INJECT ─────────────────────────────────────────────────────────────────

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void setCurrentSiteCode(String siteCode) {
        this.currentSiteCode = siteCode;
    }

    // ─── XỬ LÝ SALE REQUEST ──────────────────────────────────────────────────────

    /**
     * Bộ phận đặt hàng quốc tế kích hoạt xử lý một sale request.
     * Chạy thuật toán chọn site và tạo các site_order.
     *
     * @param saleRequestId id của yêu cầu cần xử lý
     * @return ProcessResult chứa đơn tạo được + danh sách lỗi (nếu có)
     */
    public ProcessResult processSaleRequest(int saleRequestId)
            throws IllegalStateException, SQLException {

        requireAnyRole(User.Role.OVERSEAS_ORDER_DEPT, User.Role.ADMIN);
        return overseasOrderService.processSaleRequest(saleRequestId, currentUser.getId());
    }

    // ─── XEM ĐƠN HÀNG ────────────────────────────────────────────────────────────

    /**
     * Bộ phận đặt hàng / admin xem tất cả site_order của một sale_request.
     */
    public List<SiteOrderDTO> getSiteOrdersBySaleRequest(int saleRequestId) throws SQLException {
        if (currentUser != null && currentUser.getRole() == User.Role.SALES_DEPARTMENT) {
            SaleRequestDTO dto = saleRequestService.getRequestById(saleRequestId);
            if (dto.getCreatedBy() != currentUser.getId()) {
                throw new IllegalStateException("Bạn không có quyền xem đơn hàng của yêu cầu này");
            }
        } else {
            requireAnyRole(User.Role.OVERSEAS_ORDER_DEPT, User.Role.ADMIN);
        }
        return overseasOrderService.getSiteOrdersBySaleRequest(saleRequestId);
    }

    public List<SiteOrderDTO> getAllSiteOrders() throws SQLException {
        requireAnyRole(User.Role.OVERSEAS_ORDER_DEPT, User.Role.WAREHOUSE_MANAGER, User.Role.ADMIN);
        return overseasOrderService.getAllSiteOrders();
    }

    public SiteOrderDTO createManualSiteOrder(int saleRequestId,
                                              String siteCode,
                                              SiteOrder.DeliveryMeans deliveryMeans,
                                              LocalDate estimatedDeliveryDate,
                                              List<SiteOrderItemDTO> items) throws SQLException {
        requireAnyRole(User.Role.OVERSEAS_ORDER_DEPT, User.Role.ADMIN);
        return overseasOrderService.createManualSiteOrder(
                saleRequestId, siteCode, deliveryMeans, estimatedDeliveryDate, items, currentUser.getId());
    }

    public void processSaleCancellationRequest(int saleRequestId) throws SQLException {
        requireAnyRole(User.Role.OVERSEAS_ORDER_DEPT, User.Role.ADMIN);
        overseasOrderService.processSaleCancellationRequest(saleRequestId);
    }

    public void cancelSaleRequestByOverseas(int saleRequestId, String reason) throws SQLException {
        requireAnyRole(User.Role.OVERSEAS_ORDER_DEPT, User.Role.ADMIN);
        overseasOrderService.cancelSaleRequestByOverseas(saleRequestId, reason);
    }

    /**
     * Site user xem danh sách đơn hàng của site mình.
     */
    public List<SiteOrderDTO> getMySiteOrders() throws SQLException {
        requireRole(User.Role.SITE_USER);
        if (currentSiteCode == null) {
            throw new IllegalStateException("Không xác định được site của bạn");
        }
        return overseasOrderService.getSiteOrdersBySite(currentSiteCode);
    }

    // ─── CẬP NHẬT TRẠNG THÁI ─────────────────────────────────────────────────────

    /**
     * Site user cập nhật trạng thái vận chuyển (IN_TRANSIT hoặc DELIVERED).
     * Trạng thái WAREHOUSE_CONFIRMED chỉ do quản lý kho xác nhận.
     *
     * @param siteOrderId id của đơn cần cập nhật
     * @param newStatus   trạng thái mới
     */
    public void updateDeliveryStatus(int siteOrderId,
                                     SiteOrder.Status newStatus) throws IllegalStateException, SQLException {
        requireRole(User.Role.SITE_USER);

        // Site chỉ được đặt 2 trạng thái này
        if (newStatus != SiteOrder.Status.IN_TRANSIT
                && newStatus != SiteOrder.Status.DELIVERED) {
            throw new IllegalArgumentException(
                    "Site chỉ được cập nhật trạng thái IN_TRANSIT hoặc DELIVERED");
        }

        overseasOrderService.updateSiteOrderStatus(siteOrderId, newStatus, currentSiteCode);
    }

    public void cancelMySiteOrder(int siteOrderId, String reason) throws SQLException {
        requireRole(User.Role.SITE_USER);
        overseasOrderService.cancelSiteOrderBySite(siteOrderId, currentSiteCode, reason);
    }

    public List<InventoryUpdateRequestDTO> getMyInventoryUpdateRequests() throws SQLException {
        requireRole(User.Role.SITE_USER);
        if (currentSiteCode == null) {
            throw new IllegalStateException("Không xác định được site của bạn");
        }
        return saleRequestService.getInventoryUpdateRequestsBySite(currentSiteCode);
    }

    public void completeInventoryUpdateRequest(int inventoryUpdateRequestId,
                                               SiteMerchandise updatedInventory) throws SQLException {
        requireRole(User.Role.SITE_USER);
        if (currentSiteCode == null) {
            throw new IllegalStateException("Không xác định được site của bạn");
        }
        saleRequestService.completeInventoryUpdateRequest(inventoryUpdateRequestId, currentSiteCode, updatedInventory);
    }

    /**
     * Quản lý kho xác nhận hàng về kho, điền ngày thực tế.
     *
     * @param siteOrderId       id của đơn hàng đã về kho
     * @param actualDeliveryDate ngày thực tế nhận hàng
     */
    public void confirmWarehouseReceipt(int siteOrderId,
                                        LocalDate actualDeliveryDate,
                                        List<SiteOrderItemDTO> reportedItems) throws IllegalStateException, SQLException {
        requireAnyRole(User.Role.WAREHOUSE_MANAGER, User.Role.ADMIN);
        overseasOrderService.confirmWarehouseReceipt(siteOrderId, actualDeliveryDate, reportedItems);
    }

    public void confirmSaleRequestSuccess(int saleRequestId) throws IllegalStateException, SQLException {
        requireAnyRole(User.Role.OVERSEAS_ORDER_DEPT, User.Role.ADMIN);
        overseasOrderService.confirmSaleRequestSuccess(saleRequestId);
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────────

    private void requireRole(User.Role required) {
        if (currentUser == null) throw new IllegalStateException("Chưa đăng nhập");
        if (currentUser.getRole() != required && currentUser.getRole() != User.Role.ADMIN) {
            throw new IllegalStateException("Chức năng này yêu cầu quyền: " + required.name());
        }
    }

    private void requireAnyRole(User.Role... roles) {
        if (currentUser == null) throw new IllegalStateException("Chưa đăng nhập");
        if (currentUser.getRole() == User.Role.ADMIN) return;
        for (User.Role r : roles) {
            if (currentUser.getRole() == r) return;
        }
        throw new IllegalStateException("Bạn không có quyền thực hiện chức năng này");
    }
}
