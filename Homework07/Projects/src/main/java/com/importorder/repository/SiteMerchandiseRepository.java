package com.importorder.repository;

import com.importorder.config.DatabaseConnection;
import com.importorder.entity.SiteMerchandise;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SiteMerchandiseRepository {

    // ─── MAP ROW ────────────────────────────────────────────────────────────────

    private SiteMerchandise mapRow(ResultSet rs) throws SQLException {
        SiteMerchandise sm = new SiteMerchandise();
        sm.setSiteCode(rs.getString("site_code"));
        sm.setMerchandiseCode(rs.getString("merchandise_code"));
        sm.setQuantity(rs.getInt("quantity"));
        sm.setUnit(rs.getString("unit"));
        return sm;
    }

    // ─── QUERIES ────────────────────────────────────────────────────────────────

    public List<SiteMerchandise> findBySiteCode(String siteCode) throws SQLException {
        String sql = "SELECT * FROM site_merchandises WHERE site_code = ?";
        List<SiteMerchandise> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, siteCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Optional<SiteMerchandise> findBySiteAndMerchandise(String siteCode,
                                                               String merchandiseCode) throws SQLException {
        String sql = "SELECT * FROM site_merchandises WHERE site_code = ? AND merchandise_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, siteCode);
            ps.setString(2, merchandiseCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Lấy tất cả site có hàng (quantity > 0) cho một mặt hàng cụ thể,
     * sắp xếp theo quantity giảm dần (dùng trong thuật toán chọn site).
     */
    public List<SiteMerchandise> findAvailableSitesForMerchandise(String merchandiseCode) throws SQLException {
        String sql = """
                SELECT * FROM site_merchandises
                WHERE merchandise_code = ? AND quantity > 0
                ORDER BY quantity DESC
                """;
        List<SiteMerchandise> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, merchandiseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ─── COMMANDS ───────────────────────────────────────────────────────────────

    public void insert(SiteMerchandise sm) throws SQLException {
        String sql = """
                INSERT INTO site_merchandises (site_code, merchandise_code, quantity, unit)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sm.getSiteCode());
            ps.setString(2, sm.getMerchandiseCode());
            ps.setInt(3, sm.getQuantity());
            ps.setString(4, sm.getUnit());
            ps.executeUpdate();
        }
    }

    public void insert(Connection conn, SiteMerchandise sm) throws SQLException {
        String sql = """
                INSERT INTO site_merchandises (site_code, merchandise_code, quantity, unit)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sm.getSiteCode());
            ps.setString(2, sm.getMerchandiseCode());
            ps.setInt(3, sm.getQuantity());
            ps.setString(4, sm.getUnit());
            ps.executeUpdate();
        }
    }

    /**
     * Site cập nhật số lượng tồn kho của một mặt hàng.
     */
    public void updateQuantity(String siteCode, String merchandiseCode, int newQuantity) throws SQLException {
        String sql = """
                UPDATE site_merchandises
                SET quantity = ?
                WHERE site_code = ? AND merchandise_code = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newQuantity);
            ps.setString(2, siteCode);
            ps.setString(3, merchandiseCode);
            ps.executeUpdate();
        }
    }

    public void update(SiteMerchandise siteMerchandise) throws SQLException {
        String sql = """
                UPDATE site_merchandises
                SET quantity = ?, unit = ?
                WHERE site_code = ? AND merchandise_code = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, siteMerchandise.getQuantity());
            ps.setString(2, siteMerchandise.getUnit());
            ps.setString(3, siteMerchandise.getSiteCode());
            ps.setString(4, siteMerchandise.getMerchandiseCode());
            ps.executeUpdate();
        }
    }

    public void update(Connection conn, SiteMerchandise siteMerchandise) throws SQLException {
        String sql = """
                UPDATE site_merchandises
                SET quantity = ?, unit = ?
                WHERE site_code = ? AND merchandise_code = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, siteMerchandise.getQuantity());
            ps.setString(2, siteMerchandise.getUnit());
            ps.setString(3, siteMerchandise.getSiteCode());
            ps.setString(4, siteMerchandise.getMerchandiseCode());
            ps.executeUpdate();
        }
    }

    /**
     * Giảm tồn kho sau khi đặt hàng thành công.
     * Dùng trong transaction cùng với insert SiteOrder.
     */
    public void decreaseQuantity(Connection conn, String siteCode,
                                 String merchandiseCode, int amount) throws SQLException {
        String sql = """
                UPDATE site_merchandises
                SET quantity = quantity - ?
                WHERE site_code = ? AND merchandise_code = ? AND quantity >= ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setString(2, siteCode);
            ps.setString(3, merchandiseCode);
            ps.setInt(4, amount);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException(
                        "Không đủ hàng tại site [" + siteCode + "] cho mặt hàng [" + merchandiseCode + "]");
            }
        }
    }

    public void delete(String siteCode, String merchandiseCode) throws SQLException {
        String sql = "DELETE FROM site_merchandises WHERE site_code = ? AND merchandise_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, siteCode);
            ps.setString(2, merchandiseCode);
            ps.executeUpdate();
        }
    }
}
