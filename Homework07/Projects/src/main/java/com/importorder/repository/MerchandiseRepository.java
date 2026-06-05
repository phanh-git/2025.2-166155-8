package com.importorder.repository;

import com.importorder.config.DatabaseConnection;
import com.importorder.entity.Merchandise;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MerchandiseRepository {

    // ─── MAP ROW ────────────────────────────────────────────────────────────────

    private Merchandise mapRow(ResultSet rs) throws SQLException {
        Merchandise m = new Merchandise();
        m.setId(rs.getInt("id"));
        m.setCode(rs.getString("code"));
        m.setName(rs.getString("name"));
        m.setUnit(rs.getString("unit"));
        return m;
    }

    // ─── QUERIES ────────────────────────────────────────────────────────────────

    public Optional<Merchandise> findByCode(String code) throws SQLException {
        String sql = "SELECT * FROM merchandises WHERE code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Merchandise> findById(int id) throws SQLException {
        String sql = "SELECT * FROM merchandises WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Merchandise> findAll() throws SQLException {
        String sql = "SELECT * FROM merchandises ORDER BY code";
        List<Merchandise> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Merchandise> findByNameContaining(String keyword) throws SQLException {
        String sql = "SELECT * FROM merchandises WHERE name ILIKE ? ORDER BY code";
        List<Merchandise> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Merchandise> searchByKeyword(String keyword) throws SQLException {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isBlank()) {
            return findAll();
        }

        String sql = """
                SELECT * FROM merchandises
                WHERE code ILIKE ? OR name ILIKE ?
                ORDER BY code
                """;
        List<Merchandise> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + normalizedKeyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ─── COMMANDS ───────────────────────────────────────────────────────────────

    public int insert(Merchandise merchandise) throws SQLException {
        String sql = "INSERT INTO merchandises (code, name, unit) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, merchandise.getCode());
            ps.setString(2, merchandise.getName());
            ps.setString(3, merchandise.getUnit());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Insert merchandise thất bại");
    }

    public void update(Merchandise merchandise) throws SQLException {
        String sql = "UPDATE merchandises SET name = ?, unit = ? WHERE code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, merchandise.getName());
            ps.setString(2, merchandise.getUnit());
            ps.setString(3, merchandise.getCode());
            ps.executeUpdate();
        }
    }

    public void delete(String code) throws SQLException {
        String sql = "DELETE FROM merchandises WHERE code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.executeUpdate();
        }
    }
}
