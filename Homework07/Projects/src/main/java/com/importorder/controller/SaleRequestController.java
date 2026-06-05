package com.importorder.controller;

import com.importorder.dto.SaleRequestDTO;
import com.importorder.dto.SaleRequestItemDTO;
import com.importorder.entity.Merchandise;
import com.importorder.entity.User;
import com.importorder.service.SaleRequestService;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller cho Bộ phận bán hàng.
 *
 * Trong JavaFX, controller này sẽ được khởi tạo bởi FXMLLoader,
 * hoặc khởi tạo thủ công và gọi từ các scene handler.
 *
 * Quy ước:
 *   - Mỗi method trả về dữ liệu hoặc ném exception — KHÔNG xử lý UI ở đây.
 *   - Tầng View (JavaFX) bắt exception và hiển thị Alert/thông báo lỗi.
 *   - currentUser được inject sau khi login thành công.
 */
public class SaleRequestController {

    private final SaleRequestService saleRequestService = new SaleRequestService();

    /** User đang đăng nhập, được set từ LoginController sau khi xác thực */
    private User currentUser;

    // ─── INJECT ─────────────────────────────────────────────────────────────────

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    // ─── CREATE ─────────────────────────────────────────────────────────────────

    /**
     * Gửi yêu cầu đặt hàng mới.
     * View truyền vào danh sách DTO items đã được điền từ form.
     *
     * @param items danh sách mặt hàng cần đặt
     * @return      SaleRequestDTO đã tạo (để View hiển thị xác nhận)
     * @throws IllegalArgumentException nếu dữ liệu không hợp lệ
     * @throws SQLException             nếu lỗi database
     */
    public SaleRequestDTO submitRequest(List<SaleRequestItemDTO> items)
            throws IllegalArgumentException, SQLException {

        requireRole(User.Role.SALES_DEPARTMENT);
        return saleRequestService.createRequest(currentUser.getId(), items);
    }

    // ─── READ ────────────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách yêu cầu của chính mình (bộ phận bán hàng).
     */
    public List<SaleRequestDTO> getMyRequests() throws SQLException {
        requireRole(User.Role.SALES_DEPARTMENT);
        return saleRequestService.getRequestsByUser(currentUser.getId());
    }

    /**
     * Lấy toàn bộ yêu cầu — dành cho bộ phận đặt hàng quốc tế và admin.
     */
    public List<SaleRequestDTO> getAllRequests() throws SQLException {
        requireAnyRole(User.Role.OVERSEAS_ORDER_DEPT, User.Role.ADMIN);
        return saleRequestService.getAllRequests();
    }

    /**
     * Xem chi tiết một yêu cầu.
     */
    public SaleRequestDTO getRequestDetail(int requestId) throws SQLException {
        SaleRequestDTO dto = saleRequestService.getRequestById(requestId);

        // Bộ phận bán hàng chỉ xem được yêu cầu của mình
        if (currentUser.getRole() == User.Role.SALES_DEPARTMENT
                && dto.getCreatedBy() != currentUser.getId()) {
            throw new IllegalStateException("Bạn không có quyền xem yêu cầu này");
        }

        return dto;
    }

    public List<Merchandise> searchMerchandiseCatalog(String keyword) throws SQLException {
        requireRole(User.Role.SALES_DEPARTMENT);
        return saleRequestService.searchMerchandiseCatalog(keyword);
    }

    public Merchandise createMerchandise(Merchandise merchandise) throws SQLException {
        requireRole(User.Role.SALES_DEPARTMENT);
        return saleRequestService.createMerchandise(merchandise);
    }

    public void requestSitesToUpdateInventoryForMerchandise(String merchandiseCode) throws SQLException {
        requireRole(User.Role.SALES_DEPARTMENT);
        saleRequestService.requestSitesToUpdateInventoryForMerchandise(merchandiseCode);
    }

    // ─── CANCEL ──────────────────────────────────────────────────────────────────

    /**
     * Hủy yêu cầu đặt hàng.
     *
     * @param requestId id của yêu cầu cần hủy
     * @throws IllegalStateException nếu không đủ quyền hoặc trạng thái không cho phép hủy
     */
    public void cancelRequest(int requestId) throws IllegalStateException, SQLException {
        throw new UnsupportedOperationException("Hãy dùng requestCancellation.");
    }

    public void requestCancellation(int requestId, String reason) throws IllegalStateException, SQLException {
        requireRole(User.Role.SALES_DEPARTMENT);
        saleRequestService.requestCancellation(requestId, currentUser.getId(), reason);
    }

    public void cancelRequestByOverseas(int requestId, String resolutionNote) throws IllegalStateException, SQLException {
        requireAnyRole(User.Role.OVERSEAS_ORDER_DEPT, User.Role.ADMIN);
        saleRequestService.cancelRequestByOverseas(requestId, resolutionNote);
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────────

    private void requireRole(User.Role required) {
        if (currentUser == null) {
            throw new IllegalStateException("Chưa đăng nhập");
        }
        if (currentUser.getRole() != required && currentUser.getRole() != User.Role.ADMIN) {
            throw new IllegalStateException(
                    "Chức năng này yêu cầu quyền: " + required.name());
        }
    }

    private void requireAnyRole(User.Role... roles) {
        if (currentUser == null) {
            throw new IllegalStateException("Chưa đăng nhập");
        }
        if (currentUser.getRole() == User.Role.ADMIN) return;
        for (User.Role r : roles) {
            if (currentUser.getRole() == r) return;
        }
        throw new IllegalStateException("Bạn không có quyền thực hiện chức năng này");
    }
}
