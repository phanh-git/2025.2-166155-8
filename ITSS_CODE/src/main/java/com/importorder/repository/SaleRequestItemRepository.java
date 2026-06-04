package com.importorder.repository;

import com.importorder.config.DatabaseConnection;
import com.importorder.entity.SaleRequestItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaleRequestItemRepository {

    // ─── MAP ROW ────────────────────────────────────────────────────────────────

    private SaleRequestItem mapRow(ResultSet rs) throws SQLException {
        SaleRequestItem item = new SaleRequestItem();
        item.setId(rs.getInt("id"));
        item.setSaleRequestId(rs.getInt("sale_request_id"));
        item.setMerchandiseCode(rs.getString("merchandise_code"));
        item.setQuantityOrdered(rs.getInt("quantity_ordered"));
        item.setUnit(rs.getString("unit"));
        Date d = rs.getDate("desired_delivery_date");
        if (d != null) item.setDesiredDeliveryDate(d.toLocalDate());
        return item;
    }

    // ─── QUERIES ────────────────────────────────────────────────────────────────

    public List<SaleRequestItem> findBySaleRequestId(int saleRequestId) throws SQLException {
        String sql = "SELECT * FROM sale_request_items WHERE sale_request_id = ?";
        List<SaleRequestItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleRequestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Optional<SaleRequestItem> findById(int id) throws SQLException {
        String sql = "SELECT * FROM sale_request_items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    // ─── COMMANDS ───────────────────────────────────────────────────────────────

    /**
     * Insert một item, nhận conn từ ngoài để dùng chung transaction với SaleRequest.
     */
    public int insert(Connection conn, SaleRequestItem item) throws SQLException {
        String sql = """
                INSERT INTO sale_request_items
                    (sale_request_id, merchandise_code, quantity_ordered, unit, desired_delivery_date)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getSaleRequestId());
            ps.setString(2, item.getMerchandiseCode());
            ps.setInt(3, item.getQuantityOrdered());
            ps.setString(4, item.getUnit());
            ps.setDate(5, item.getDesiredDeliveryDate() != null
                    ? Date.valueOf(item.getDesiredDeliveryDate()) : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Insert sale_request_item thất bại");
    }

    /**
     * Insert cả danh sách items trong cùng một transaction.
     */
    public void insertAll(Connection conn, List<SaleRequestItem> items) throws SQLException {
        for (SaleRequestItem item : items) {
            insert(conn, item);
        }
    }
}