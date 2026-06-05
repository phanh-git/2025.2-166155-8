package com.importorder.repository;

import com.importorder.config.DatabaseConnection;
import com.importorder.entity.InventoryUpdateRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InventoryUpdateRequestRepository {

    private InventoryUpdateRequest mapRow(ResultSet rs) throws SQLException {
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setId(rs.getInt("id"));
        request.setMerchandiseCode(rs.getString("merchandise_code"));
        request.setSiteCode(rs.getString("site_code"));
        request.setStatus(InventoryUpdateRequest.Status.valueOf(rs.getString("status")));
        Timestamp requestedAt = rs.getTimestamp("requested_at");
        if (requestedAt != null) {
            request.setRequestedAt(requestedAt.toLocalDateTime());
        }
        Timestamp respondedAt = rs.getTimestamp("responded_at");
        if (respondedAt != null) {
            request.setRespondedAt(respondedAt.toLocalDateTime());
        }
        return request;
    }

    public List<InventoryUpdateRequest> findByMerchandiseCode(String merchandiseCode) throws SQLException {
        String sql = """
                SELECT * FROM merchandise_inventory_update_requests
                WHERE merchandise_code = ?
                ORDER BY requested_at, site_code
                """;
        List<InventoryUpdateRequest> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, merchandiseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    public List<InventoryUpdateRequest> findBySiteCode(String siteCode) throws SQLException {
        String sql = """
                SELECT * FROM merchandise_inventory_update_requests
                WHERE site_code = ?
                ORDER BY status, requested_at DESC, id DESC
                """;
        List<InventoryUpdateRequest> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, siteCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    public Optional<InventoryUpdateRequest> findById(int id) throws SQLException {
        String sql = "SELECT * FROM merchandise_inventory_update_requests WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public boolean existsByMerchandiseCode(String merchandiseCode) throws SQLException {
        String sql = "SELECT 1 FROM merchandise_inventory_update_requests WHERE merchandise_code = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, merchandiseCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insert(Connection conn, InventoryUpdateRequest request) throws SQLException {
        String sql = """
                INSERT INTO merchandise_inventory_update_requests
                    (merchandise_code, site_code, status, requested_at, responded_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, request.getMerchandiseCode());
            ps.setString(2, request.getSiteCode());
            ps.setString(3, request.getStatus().name());
            ps.setTimestamp(4, request.getRequestedAt() != null ? Timestamp.valueOf(request.getRequestedAt()) : null);
            ps.setTimestamp(5, request.getRespondedAt() != null ? Timestamp.valueOf(request.getRespondedAt()) : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insert inventory_update_request thất bại");
    }

    public void markCompleted(Connection conn, int id) throws SQLException {
        String sql = """
                UPDATE merchandise_inventory_update_requests
                SET status = ?, responded_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, InventoryUpdateRequest.Status.COMPLETED.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
