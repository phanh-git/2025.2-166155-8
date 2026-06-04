package com.importorder.repository;

import com.importorder.config.DatabaseConnection;
import com.importorder.entity.SaleRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaleRequestRepository {

    // ─── MAP ROW ────────────────────────────────────────────────────────────────

    private SaleRequest mapRow(ResultSet rs) throws SQLException {
        SaleRequest r = new SaleRequest();
        r.setId(rs.getInt("id"));
        r.setCreatedBy(rs.getInt("created_by"));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        r.setStatus(SaleRequest.Status.valueOf(rs.getString("status")));
        r.setCancelRequested(rs.getBoolean("cancel_requested"));
        r.setCancelRequestReason(rs.getString("cancel_request_reason"));
        r.setCancelResolutionNote(rs.getString("cancel_resolution_note"));
        return r;
    }

    // ─── QUERIES ────────────────────────────────────────────────────────────────

    public Optional<SaleRequest> findById(int id) throws SQLException {
        String sql = "SELECT * FROM sale_requests WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<SaleRequest> findAll() throws SQLException {
        String sql = "SELECT * FROM sale_requests ORDER BY created_at DESC";
        List<SaleRequest> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<SaleRequest> findByCreatedBy(int userId) throws SQLException {
        String sql = "SELECT * FROM sale_requests WHERE created_by = ? ORDER BY created_at DESC";
        List<SaleRequest> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<SaleRequest> findByStatus(SaleRequest.Status status) throws SQLException {
        String sql = "SELECT * FROM sale_requests WHERE status = ? ORDER BY created_at DESC";
        List<SaleRequest> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ─── COMMANDS ───────────────────────────────────────────────────────────────

    public int insert(SaleRequest request) throws SQLException {
        String sql = """
                INSERT INTO sale_requests (created_by, created_at, status, cancel_requested, cancel_request_reason, cancel_resolution_note)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, request.getCreatedBy());
            ps.setTimestamp(2, Timestamp.valueOf(request.getCreatedAt()));
            ps.setString(3, request.getStatus().name());
            ps.setBoolean(4, request.isCancelRequested());
            ps.setString(5, request.getCancelRequestReason());
            ps.setString(6, request.getCancelResolutionNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Insert sale_request thất bại");
    }

    public void updateStatus(int id, SaleRequest.Status status) throws SQLException {
        String sql = "UPDATE sale_requests SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void requestCancellation(int id, String requestReason) throws SQLException {
        String sql = """
                UPDATE sale_requests
                SET cancel_requested = TRUE,
                    cancel_request_reason = ?,
                    cancel_resolution_note = NULL
                WHERE id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requestReason);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void resolveCancellationRequest(int id, boolean cancelRequested, String resolutionNote) throws SQLException {
        String sql = """
                UPDATE sale_requests
                SET cancel_requested = ?,
                    cancel_resolution_note = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, cancelRequested);
            ps.setString(2, resolutionNote);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void updateStatusAndCancellation(int id,
                                            SaleRequest.Status status,
                                            boolean cancelRequested,
                                            String requestReason,
                                            String resolutionNote) throws SQLException {
        String sql = """
                UPDATE sale_requests
                SET status = ?,
                    cancel_requested = ?,
                    cancel_request_reason = ?,
                    cancel_resolution_note = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setBoolean(2, cancelRequested);
            ps.setString(3, requestReason);
            ps.setString(4, resolutionNote);
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }
}
