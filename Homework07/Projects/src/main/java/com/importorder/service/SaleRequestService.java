package com.importorder.service;

import com.importorder.config.DatabaseConnection;
import com.importorder.dto.InventoryUpdateRequestDTO;
import com.importorder.dto.SaleRequestDTO;
import com.importorder.dto.SaleRequestItemDTO;
import com.importorder.entity.InventoryUpdateRequest;
import com.importorder.entity.Merchandise;
import com.importorder.entity.SaleRequest;
import com.importorder.entity.SaleRequestItem;
import com.importorder.entity.Site;
import com.importorder.entity.SiteMerchandise;
import com.importorder.entity.User;
import com.importorder.repository.InventoryUpdateRequestRepository;
import com.importorder.repository.MerchandiseRepository;
import com.importorder.repository.SaleRequestItemRepository;
import com.importorder.repository.SaleRequestRepository;
import com.importorder.repository.SiteMerchandiseRepository;
import com.importorder.repository.SiteRepository;
import com.importorder.repository.UserRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service cho Bộ phận bán hàng:
 * - Tạo yêu cầu đặt hàng mới
 * - Xem danh sách / chi tiết yêu cầu của mình
 * - Hủy yêu cầu (khi chưa bắt đầu xử lý)
 */
public class SaleRequestService {

    private final SaleRequestRepository saleRequestRepo = new SaleRequestRepository();
    private final SaleRequestItemRepository saleRequestItemRepo = new SaleRequestItemRepository();
    private final MerchandiseRepository merchandiseRepo = new MerchandiseRepository();
    private final UserRepository userRepo = new UserRepository();
    private final SiteRepository siteRepo = new SiteRepository();
    private final SiteMerchandiseRepository siteMerchandiseRepo = new SiteMerchandiseRepository();
    private final InventoryUpdateRequestRepository inventoryUpdateRequestRepo = new InventoryUpdateRequestRepository();

    // ─── CREATE ─────────────────────────────────────────────────────────────────

    /**
     * Tạo mới một yêu cầu đặt hàng cùng toàn bộ items trong một transaction.
     *
     * @param createdByUserId   id của user bộ phận bán hàng
     * @param items             danh sách mặt hàng cần đặt
     * @return                  SaleRequestDTO đã lưu (có id)
     */
    public SaleRequestDTO createRequest(int createdByUserId,
                                        List<SaleRequestItemDTO> items) throws SQLException {
        // Validate items không rỗng
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mặt hàng không được rỗng");
        }

        // Validate từng merchandise code tồn tại
        for (SaleRequestItemDTO item : items) {
            Optional<Merchandise> m = merchandiseRepo.findByCode(item.getMerchandiseCode());
            if (m.isEmpty()) {
                throw new IllegalArgumentException(
                        "Mã hàng không tồn tại: " + item.getMerchandiseCode());
            }
            if (item.getQuantityOrdered() <= 0) {
                throw new IllegalArgumentException(
                        "Số lượng phải lớn hơn 0 cho mặt hàng: " + item.getMerchandiseCode());
            }
            if (item.getDesiredDeliveryDate() == null) {
                throw new IllegalArgumentException(
                        "Ngày nhận mong muốn không được bỏ trống: " + item.getMerchandiseCode());
            }
        }

        // Dùng transaction để insert header + items cùng lúc
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insert SaleRequest header
                SaleRequest request = new SaleRequest();
                request.setCreatedBy(createdByUserId);
                request.setCreatedAt(LocalDateTime.now());
                request.setStatus(SaleRequest.Status.RECEIVED);

                // Insert qua repo dùng connection riêng — ở đây dùng overload nhận conn
                String sql = """
                        INSERT INTO sale_requests (created_by, created_at, status)
                        VALUES (?, ?, ?)
                        """;
                int requestId;
                try (var ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, request.getCreatedBy());
                    ps.setTimestamp(2, java.sql.Timestamp.valueOf(request.getCreatedAt()));
                    ps.setString(3, request.getStatus().name());
                    ps.executeUpdate();
                    try (var keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không lấy được id SaleRequest");
                        requestId = keys.getInt(1);
                    }
                }
                request.setId(requestId);

                // 2. Insert từng item
                List<SaleRequestItem> entityItems = new ArrayList<>();
                for (SaleRequestItemDTO dto : items) {
                    SaleRequestItem item = new SaleRequestItem();
                    item.setSaleRequestId(requestId);
                    item.setMerchandiseCode(dto.getMerchandiseCode());
                    item.setQuantityOrdered(dto.getQuantityOrdered());
                    item.setUnit(dto.getUnit());
                    item.setDesiredDeliveryDate(dto.getDesiredDeliveryDate());
                    int itemId = saleRequestItemRepo.insert(conn, item);
                    item.setId(itemId);
                    entityItems.add(item);
                }

                conn.commit();

                // 3. Build và trả về DTO
                return buildDTO(request, entityItems);

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ─── READ ────────────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách tất cả yêu cầu của một user bán hàng.
     */
    public List<SaleRequestDTO> getRequestsByUser(int userId) throws SQLException {
        List<SaleRequest> requests = saleRequestRepo.findByCreatedBy(userId);
        List<SaleRequestDTO> result = new ArrayList<>();
        for (SaleRequest r : requests) {
            List<SaleRequestItem> items = saleRequestItemRepo.findBySaleRequestId(r.getId());
            result.add(buildDTO(r, items));
        }
        return result;
    }

    /**
     * Lấy toàn bộ yêu cầu (dành cho bộ phận đặt hàng quốc tế / admin).
     */
    public List<SaleRequestDTO> getAllRequests() throws SQLException {
        List<SaleRequest> requests = saleRequestRepo.findAll();
        List<SaleRequestDTO> result = new ArrayList<>();
        for (SaleRequest r : requests) {
            List<SaleRequestItem> items = saleRequestItemRepo.findBySaleRequestId(r.getId());
            result.add(buildDTO(r, items));
        }
        return result;
    }

    public SaleRequestDTO getRequestById(int id) throws SQLException {
        SaleRequest request = saleRequestRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sale request id=" + id));
        List<SaleRequestItem> items = saleRequestItemRepo.findBySaleRequestId(id);
        return buildDTO(request, items);
    }

    // ─── CANCEL ──────────────────────────────────────────────────────────────────

    /**
     * Hủy yêu cầu — chỉ được hủy khi chưa bắt đầu xử lý (status = RECEIVED).
     */
    public void cancelRequest(int requestId, int requestedByUserId) throws SQLException {
        throw new UnsupportedOperationException("Hãy dùng requestCancellation để gửi yêu cầu hủy.");
    }

    public void requestCancellation(int requestId,
                                    int requestedByUserId,
                                    String requestReason) throws SQLException {
        SaleRequest request = saleRequestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sale request id=" + requestId));

        if (request.getCreatedBy() != requestedByUserId) {
            throw new IllegalStateException("Bạn không có quyền hủy yêu cầu này");
        }
        if (request.getStatus() == SaleRequest.Status.SUCCESS) {
            throw new IllegalStateException("Không thể gửi yêu cầu hủy cho yêu cầu đã hoàn thành");
        }
        if (request.getStatus() == SaleRequest.Status.CANCELLED) {
            throw new IllegalStateException("Yêu cầu này đã bị hủy");
        }
        if (request.isCancelRequested()) {
            throw new IllegalStateException("Yêu cầu này đang chờ bộ phận đặt hàng xử lý hủy");
        }
        if (requestReason == null || requestReason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy");
        }

        saleRequestRepo.requestCancellation(requestId, requestReason.trim());
    }

    public void cancelRequestByOverseas(int requestId, String resolutionNote) throws SQLException {
        SaleRequest request = saleRequestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sale request id=" + requestId));

        if (request.getStatus() == SaleRequest.Status.SUCCESS) {
            throw new IllegalStateException("Không thể hủy yêu cầu đã hoàn thành");
        }
        if (request.getStatus() == SaleRequest.Status.CANCELLED) {
            throw new IllegalStateException("Yêu cầu này đã bị hủy");
        }

        saleRequestRepo.updateStatusAndCancellation(
                requestId,
                SaleRequest.Status.CANCELLED,
                false,
                request.getCancelRequestReason(),
                resolutionNote
        );
    }

    public Merchandise createMerchandise(Merchandise merchandise) throws SQLException {
        String name = merchandise.getName() != null ? merchandise.getName().trim() : "";
        String unit = merchandise.getUnit() != null ? merchandise.getUnit().trim() : "";
        if (name.isBlank()) {
            throw new IllegalArgumentException("Tên mặt hàng không được để trống");
        }

        Merchandise toSave = new Merchandise();
        toSave.setName(name);
        toSave.setUnit(unit.isBlank() ? null : unit);
        toSave.setCode(generateNextMerchandiseCode());
        int id = merchandiseRepo.insert(toSave);
        toSave.setId(id);
        return toSave;
    }

    public List<Merchandise> searchMerchandiseCatalog(String keyword) throws SQLException {
        return merchandiseRepo.searchByKeyword(keyword);
    }

    public void requestSitesToUpdateInventoryForMerchandise(String merchandiseCode) throws SQLException {
        Merchandise merchandise = merchandiseRepo.findByCode(merchandiseCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mặt hàng " + merchandiseCode));
        if (inventoryUpdateRequestRepo.existsByMerchandiseCode(merchandise.getCode())) {
            throw new IllegalStateException("Đã gửi yêu cầu cập nhật tồn kho cho mặt hàng này trước đó");
        }

        List<Site> sites = siteRepo.findAll();
        if (sites.isEmpty()) {
            throw new IllegalStateException("Chưa có site nào để gửi yêu cầu");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (Site site : sites) {
                    InventoryUpdateRequest inventoryUpdateRequest = new InventoryUpdateRequest();
                    inventoryUpdateRequest.setMerchandiseCode(merchandise.getCode());
                    inventoryUpdateRequest.setSiteCode(site.getCode());
                    inventoryUpdateRequest.setStatus(InventoryUpdateRequest.Status.PENDING);
                    inventoryUpdateRequest.setRequestedAt(LocalDateTime.now());
                    inventoryUpdateRequestRepo.insert(conn, inventoryUpdateRequest);
                }
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<InventoryUpdateRequestDTO> getInventoryUpdateRequestsBySite(String siteCode) throws SQLException {
        List<InventoryUpdateRequest> requests = inventoryUpdateRequestRepo.findBySiteCode(siteCode);
        List<InventoryUpdateRequestDTO> result = new ArrayList<>();
        for (InventoryUpdateRequest request : requests) {
            result.add(buildInventoryUpdateRequestDTO(request));
        }
        return result;
    }

    public void completeInventoryUpdateRequest(int inventoryUpdateRequestId,
                                               String siteCode,
                                               SiteMerchandise updatedInventory) throws SQLException {
        InventoryUpdateRequest request = inventoryUpdateRequestRepo.findById(inventoryUpdateRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu cập nhật tồn kho"));
        if (!request.getSiteCode().equals(siteCode)) {
            throw new IllegalStateException("Bạn không có quyền xử lý yêu cầu này");
        }
        if (request.getStatus() == InventoryUpdateRequest.Status.COMPLETED) {
            throw new IllegalStateException("Yêu cầu này đã được phản hồi");
        }
        if (updatedInventory == null) {
            throw new IllegalArgumentException("Thông tin tồn kho cập nhật không được để trống");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (updatedInventory.getQuantity() < 0) {
                    throw new IllegalArgumentException("Số lượng tồn kho không hợp lệ cho mặt hàng " + updatedInventory.getMerchandiseCode());
                }
                updatedInventory.setSiteCode(siteCode);
                Optional<SiteMerchandise> existing = siteMerchandiseRepo.findBySiteAndMerchandise(siteCode, updatedInventory.getMerchandiseCode());
                if (existing.isPresent()) {
                    siteMerchandiseRepo.update(conn, updatedInventory);
                } else {
                    siteMerchandiseRepo.insert(conn, updatedInventory);
                }
                inventoryUpdateRequestRepo.markCompleted(conn, inventoryUpdateRequestId);
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ─── MAPPER ──────────────────────────────────────────────────────────────────

    private SaleRequestDTO buildDTO(SaleRequest request, List<SaleRequestItem> items) throws SQLException {
        SaleRequestDTO dto = new SaleRequestDTO();
        dto.setId(request.getId());
        dto.setCreatedBy(request.getCreatedBy());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setStatus(request.getStatus());
        dto.setCancelRequested(request.isCancelRequested());
        dto.setCancelRequestReason(request.getCancelRequestReason());
        dto.setCancelResolutionNote(request.getCancelResolutionNote());
        userRepo.findById(request.getCreatedBy())
                .map(User::getUsername)
                .ifPresent(dto::setCreatedByUsername);

        List<SaleRequestItemDTO> itemDTOs = new ArrayList<>();
        for (SaleRequestItem item : items) {
            SaleRequestItemDTO itemDTO = new SaleRequestItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setSaleRequestId(item.getSaleRequestId());
            itemDTO.setMerchandiseCode(item.getMerchandiseCode());
            itemDTO.setQuantityOrdered(item.getQuantityOrdered());
            itemDTO.setUnit(item.getUnit());
            itemDTO.setDesiredDeliveryDate(item.getDesiredDeliveryDate());

            // Enrich tên hàng
            merchandiseRepo.findByCode(item.getMerchandiseCode())
                    .ifPresent(m -> itemDTO.setMerchandiseName(m.getName()));

            itemDTOs.add(itemDTO);
        }
        dto.setItems(itemDTOs);
        return dto;
    }

    private InventoryUpdateRequestDTO buildInventoryUpdateRequestDTO(InventoryUpdateRequest request) throws SQLException {
        InventoryUpdateRequestDTO dto = new InventoryUpdateRequestDTO();
        dto.setId(request.getId());
        dto.setMerchandiseCode(request.getMerchandiseCode());
        dto.setSiteCode(request.getSiteCode());
        dto.setStatus(request.getStatus());
        dto.setRequestedAt(request.getRequestedAt());
        dto.setRespondedAt(request.getRespondedAt());
        siteRepo.findByCode(request.getSiteCode()).map(Site::getName).ifPresent(dto::setSiteName);
        merchandiseRepo.findByCode(request.getMerchandiseCode()).ifPresent(merchandise -> {
            dto.setMerchandiseName(merchandise.getName());
            dto.setMerchandiseUnit(merchandise.getUnit());
        });
        return dto;
    }

    private String generateNextMerchandiseCode() throws SQLException {
        List<Merchandise> merchandises = merchandiseRepo.findAll();
        int max = 0;
        for (Merchandise item : merchandises) {
            String code = item.getCode();
            if (code != null && code.matches("P\\d+")) {
                max = Math.max(max, Integer.parseInt(code.substring(1)));
            }
        }
        return "P" + String.format("%03d", max + 1);
    }
}
