package com.importorder.repository;

import com.importorder.config.DatabaseConnection;
import com.importorder.entity.SiteOrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SiteOrderItemRepository {

    // ─── MAP ROW ────────────────────────────────────────────────────────────────

    private SiteOrderItem mapRow(ResultSet rs) throws SQLException {
        SiteOrderItem item = new SiteOrderItem();
        item.setId(rs.getInt("id"));
        item.setSiteOrderId(rs.getInt("site_order_id"));
        item.setSaleRequestItemId(rs.getInt("sale_request_item_id"));
        item.setMerchandiseCode(rs.getString("merchandise_code"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnit(rs.getString("unit"));
        int receivedQuantity = rs.getInt("received_quantity");
        if (!rs.wasNull()) {
            item.setReceivedQuantity(receivedQuantity);
        }
        item.setShortageNote(rs.getString("shortage_note"));
        return item;
    }

    // ─── QUERIES ────────────────────────────────────────────────────────────────

    public List<SiteOrderItem> findBySiteOrderId(int siteOrderId) throws SQLException {
        String sql = "SELECT * FROM site_order_items WHERE site_order_id = ?";
        List<SiteOrderItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, siteOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Tra cứu các site_order_item xuất phát từ một sale_request_item.
     * Dùng để biết mặt hàng đó đã được chia cho site nào, bao nhiêu.
     */
    public List<SiteOrderItem> findBySaleRequestItemId(int saleRequestItemId) throws SQLException {
        String sql = "SELECT * FROM site_order_items WHERE sale_request_item_id = ?";
        List<SiteOrderItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleRequestItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ─── COMMANDS ───────────────────────────────────────────────────────────────

    /**
     * Insert một item, nhận conn từ ngoài để dùng chung transaction.
     */
    public int insert(Connection conn, SiteOrderItem item) throws SQLException {
        String sql = """
                INSERT INTO site_order_items
                    (site_order_id, sale_request_item_id, merchandise_code, quantity, unit, received_quantity, shortage_note)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getSiteOrderId());
            ps.setInt(2, item.getSaleRequestItemId());
            ps.setString(3, item.getMerchandiseCode());
            ps.setInt(4, item.getQuantity());
            ps.setString(5, item.getUnit());
            if (item.getReceivedQuantity() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, item.getReceivedQuantity());
            }
            ps.setString(7, item.getShortageNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Insert site_order_item thất bại");
    }

    public void updateWarehouseReport(Connection conn, SiteOrderItem item) throws SQLException {
        String sql = """
                UPDATE site_order_items
                SET received_quantity = ?, shortage_note = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (item.getReceivedQuantity() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, item.getReceivedQuantity());
            }
            ps.setString(2, item.getShortageNote());
            ps.setInt(3, item.getId());
            ps.executeUpdate();
        }
    }
}
