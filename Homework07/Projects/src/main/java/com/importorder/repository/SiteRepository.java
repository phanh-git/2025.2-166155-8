package com.importorder.repository;

import com.importorder.config.DatabaseConnection;
import com.importorder.entity.Site;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SiteRepository {

    // ─── MAP ROW ────────────────────────────────────────────────────────────────

    private Site mapRow(ResultSet rs) throws SQLException {
        Site s = new Site();
        s.setCode(rs.getString("code"));
        s.setName(rs.getString("name"));
        s.setDeliveryDaysByShip(rs.getInt("delivery_days_by_ship"));
        s.setDeliveryDaysByAir(rs.getInt("delivery_days_by_air"));
        s.setNote(rs.getString("note"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) s.setCreatedAt(ts.toLocalDateTime());
        return s;
    }

    // ─── QUERIES ────────────────────────────────────────────────────────────────

    public Optional<Site> findByCode(String code) throws SQLException {
        String sql = "SELECT * FROM sites WHERE code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Site> findAll() throws SQLException {
        String sql = "SELECT * FROM sites ORDER BY code";
        List<Site> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Lấy các site kinh doanh ít nhất một trong các mặt hàng đã cho.
     * Dùng khi bộ phận đặt hàng tìm site phù hợp cho một sale_request.
     */
    public List<Site> findSitesByMerchandiseCodes(List<String> merchandiseCodes) throws SQLException {
        if (merchandiseCodes == null || merchandiseCodes.isEmpty()) return new ArrayList<>();

        String placeholders = "?,".repeat(merchandiseCodes.size());
        placeholders = placeholders.substring(0, placeholders.length() - 1);

        String sql = """
                SELECT DISTINCT s.*
                FROM sites s
                JOIN site_merchandises sm ON sm.site_code = s.code
                WHERE sm.merchandise_code IN (%s)
                ORDER BY s.code
                """.formatted(placeholders);

        List<Site> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < merchandiseCodes.size(); i++) {
                ps.setString(i + 1, merchandiseCodes.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ─── COMMANDS ───────────────────────────────────────────────────────────────

    public void insert(Site site) throws SQLException {
        String sql = """
                INSERT INTO sites (code, name, delivery_days_by_ship, delivery_days_by_air, note, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, site.getCode());
            ps.setString(2, site.getName());
            ps.setInt(3, site.getDeliveryDaysByShip());
            ps.setInt(4, site.getDeliveryDaysByAir());
            ps.setString(5, site.getNote());
            ps.setTimestamp(6, site.getCreatedAt() != null
                    ? Timestamp.valueOf(site.getCreatedAt()) : null);
            ps.executeUpdate();
        }
    }

    /**
     * Site tự cập nhật thông tin vận chuyển của mình.
     */
    public void update(Site site) throws SQLException {
        String sql = """
                UPDATE sites
                SET name = ?, delivery_days_by_ship = ?, delivery_days_by_air = ?, note = ?
                WHERE code = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, site.getName());
            ps.setInt(2, site.getDeliveryDaysByShip());
            ps.setInt(3, site.getDeliveryDaysByAir());
            ps.setString(4, site.getNote());
            ps.setString(5, site.getCode());
            ps.executeUpdate();
        }
    }

    public void delete(String code) throws SQLException {
        String sql = "DELETE FROM sites WHERE code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.executeUpdate();
        }
    }
}