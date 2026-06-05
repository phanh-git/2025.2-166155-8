package com.importorder.repository;

import com.importorder.config.DatabaseConnection;
import com.importorder.entity.SiteOrder;
import com.importorder.entity.SiteOrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SiteOrderRepository {

    private final SiteOrderItemRepository itemRepo = new SiteOrderItemRepository();

    // ─── MAP ROW ────────────────────────────────────────────────────────────────

    private SiteOrder mapRow(ResultSet rs) throws SQLException {
        SiteOrder o = new SiteOrder();
        o.setId(rs.getInt("id"));
        o.setSaleRequestId(rs.getInt("sale_request_id"));
        o.setSiteCode(rs.getString("site_code"));
        o.setStatus(SiteOrder.Status.valueOf(rs.getString("status")));
        o.setDeliveryMeans(SiteOrder.DeliveryMeans.valueOf(rs.getString("delivery_means")));
        Date est = rs.getDate("estimated_delivery_date");
        if (est != null) o.setEstimatedDeliveryDate(est.toLocalDate());
        Date act = rs.getDate("actual_delivery_date");
        if (act != null) o.setActualDeliveryDate(act.toLocalDate());
        o.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        o.setCreatedBy(rs.getInt("created_by"));
        o.setCancelReason(rs.getString("cancel_reason"));
        return o;
    }

    // ─── QUERIES ────────────────────────────────────────────────────────────────

    public Optional<SiteOrder> findById(int id) throws SQLException {
        String sql = "SELECT * FROM site_orders WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<SiteOrder> findBySaleRequestId(int saleRequestId) throws SQLException {
        String sql = "SELECT * FROM site_orders WHERE sale_request_id = ? ORDER BY id";
        List<SiteOrder> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleRequestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<SiteOrder> findBySiteCode(String siteCode) throws SQLException {
        String sql = "SELECT * FROM site_orders WHERE site_code = ? ORDER BY created_at DESC";
        List<SiteOrder> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, siteCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<SiteOrder> findByStatus(SiteOrder.Status status) throws SQLException {
        String sql = "SELECT * FROM site_orders WHERE status = ? ORDER BY created_at DESC";
        List<SiteOrder> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<SiteOrder> findAll() throws SQLException {
        String sql = "SELECT * FROM site_orders ORDER BY created_at DESC, id DESC";
        List<SiteOrder> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ─── COMMANDS ───────────────────────────────────────────────────────────────

    /**
     * Insert một SiteOrder, dùng conn từ ngoài để chạy chung transaction.
     */
    public int insert(Connection conn, SiteOrder order) throws SQLException {
        String sql = """
                INSERT INTO site_orders
                    (sale_request_id, site_code, status, delivery_means,
                     estimated_delivery_date, actual_delivery_date, created_at, created_by, cancel_reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getSaleRequestId());
            ps.setString(2, order.getSiteCode());
            ps.setString(3, order.getStatus().name());
            ps.setString(4, order.getDeliveryMeans().name());
            ps.setDate(5, order.getEstimatedDeliveryDate() != null
                    ? Date.valueOf(order.getEstimatedDeliveryDate()) : null);
            ps.setDate(6, order.getActualDeliveryDate() != null
                    ? Date.valueOf(order.getActualDeliveryDate()) : null);
            ps.setTimestamp(7, Timestamp.valueOf(order.getCreatedAt()));
            ps.setInt(8, order.getCreatedBy());
            ps.setString(9, order.getCancelReason());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Insert site_order thất bại");
    }

    /**
     * Tạo toàn bộ site_order + site_order_items trong một transaction.
     *
     * @param order     đơn hàng cần tạo
     * @param items     danh sách item của đơn hàng này
     * @return          id của site_order vừa tạo
     */
    public int insertWithItems(SiteOrder order, List<SiteOrderItem> items) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insert site_order
                int orderId = insert(conn, order);
                order.setId(orderId);

                // 2. Insert từng item
                for (SiteOrderItem item : items) {
                    item.setSiteOrderId(orderId);
                    itemRepo.insert(conn, item);
                }

                conn.commit();
                return orderId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void updateStatus(int id, SiteOrder.Status status) throws SQLException {
        String sql = "UPDATE site_orders SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateStatusWithReason(int id, SiteOrder.Status status, String cancelReason) throws SQLException {
        String sql = """
                UPDATE site_orders
                SET status = ?, cancel_reason = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, cancelReason);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /**
     * Quản lý kho xác nhận hàng về, cập nhật actual_delivery_date và status.
     */
    public void confirmDelivery(int id, java.time.LocalDate actualDate) throws SQLException {
        String sql = """
                UPDATE site_orders
                SET status = ?, actual_delivery_date = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, SiteOrder.Status.WAREHOUSE_CONFIRMED.name());
            ps.setDate(2, Date.valueOf(actualDate));
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void updateWarehouseStatus(int id,
                                      SiteOrder.Status status,
                                      java.time.LocalDate actualDate) throws SQLException {
        String sql = """
                UPDATE site_orders
                SET status = ?, actual_delivery_date = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setDate(2, Date.valueOf(actualDate));
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void updateWarehouseStatus(Connection conn,
                                      int id,
                                      SiteOrder.Status status,
                                      java.time.LocalDate actualDate) throws SQLException {
        String sql = """
                UPDATE site_orders
                SET status = ?, actual_delivery_date = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setDate(2, Date.valueOf(actualDate));
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }
}
