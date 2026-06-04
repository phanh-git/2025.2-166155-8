package com.importorder.repository;

import com.importorder.config.DatabaseConnection;
import com.importorder.entity.SiteUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SiteUserRepository {

    private SiteUser mapRow(ResultSet rs) throws SQLException {
        SiteUser siteUser = new SiteUser();
        siteUser.setUserId(rs.getInt("user_id"));
        siteUser.setSiteCode(rs.getString("site_code"));
        return siteUser;
    }

    public Optional<SiteUser> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM site_users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<SiteUser> findAll() throws SQLException {
        String sql = "SELECT * FROM site_users ORDER BY user_id";
        List<SiteUser> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public void upsert(SiteUser siteUser) throws SQLException {
        String sql = """
                INSERT INTO site_users (user_id, site_code)
                VALUES (?, ?)
                ON CONFLICT (user_id)
                DO UPDATE SET site_code = EXCLUDED.site_code
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, siteUser.getUserId());
            ps.setString(2, siteUser.getSiteCode());
            ps.executeUpdate();
        }
    }

    public void deleteByUserId(int userId) throws SQLException {
        String sql = "DELETE FROM site_users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
