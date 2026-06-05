package com.importorder.entity;

import java.time.LocalDateTime;

/**
 * Yêu cầu đặt hàng từ Bộ phận bán hàng gửi lên Bộ phận đặt hàng quốc tế.
 */
public class SaleRequest {

    public enum Status {
        RECEIVED,       // Bộ phận đặt hàng đã nhận
        IN_PROGRESS,    // Đang đặt hàng
        SUCCESS,        // Thành công
        CANCELLED       // Đã hủy
    }

    private int id;
    private int createdBy;          // FK -> User.id (role = SALES_DEPARTMENT)
    private LocalDateTime createdAt;
    private Status status;
    private boolean cancelRequested;
    private String cancelRequestReason;
    private String cancelResolutionNote;

    public SaleRequest() {}

    public SaleRequest(int id, int createdBy, LocalDateTime createdAt, Status status) {
        this.id = id;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public boolean isCancelRequested() { return cancelRequested; }
    public void setCancelRequested(boolean cancelRequested) { this.cancelRequested = cancelRequested; }

    public String getCancelRequestReason() { return cancelRequestReason; }
    public void setCancelRequestReason(String cancelRequestReason) { this.cancelRequestReason = cancelRequestReason; }

    public String getCancelResolutionNote() { return cancelResolutionNote; }
    public void setCancelResolutionNote(String cancelResolutionNote) { this.cancelResolutionNote = cancelResolutionNote; }

    @Override
    public String toString() {
        return "SaleRequest{id=" + id + ", status=" + status + ", createdAt=" + createdAt
                + ", cancelRequested=" + cancelRequested + "}";
    }
}
