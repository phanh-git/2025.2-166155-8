package com.importorder.dto;

import com.importorder.entity.SaleRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO đại diện cho một yêu cầu đặt hàng đầy đủ (header + items).
 * Dùng để truyền dữ liệu giữa các tầng mà không expose entity trực tiếp.
 */
public class SaleRequestDTO {

    private int id;
    private int createdBy;
    private String createdByUsername;   // join từ bảng users để hiển thị tên
    private LocalDateTime createdAt;
    private SaleRequest.Status status;
    private boolean cancelRequested;
    private String cancelRequestReason;
    private String cancelResolutionNote;
    private List<SaleRequestItemDTO> items;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public SaleRequestDTO() {}

    // ─── Getters / Setters ──────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public SaleRequest.Status getStatus() { return status; }
    public void setStatus(SaleRequest.Status status) { this.status = status; }

    public boolean isCancelRequested() { return cancelRequested; }
    public void setCancelRequested(boolean cancelRequested) { this.cancelRequested = cancelRequested; }

    public String getCancelRequestReason() { return cancelRequestReason; }
    public void setCancelRequestReason(String cancelRequestReason) { this.cancelRequestReason = cancelRequestReason; }

    public String getCancelResolutionNote() { return cancelResolutionNote; }
    public void setCancelResolutionNote(String cancelResolutionNote) { this.cancelResolutionNote = cancelResolutionNote; }

    public List<SaleRequestItemDTO> getItems() { return items; }
    public void setItems(List<SaleRequestItemDTO> items) { this.items = items; }

    @Override
    public String toString() {
        return "SaleRequestDTO{id=" + id + ", status=" + status + ", items=" + items + "}";
    }
}
