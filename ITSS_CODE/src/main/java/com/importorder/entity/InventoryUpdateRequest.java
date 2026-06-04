package com.importorder.entity;

import java.time.LocalDateTime;

/**
 * Yêu cầu sale gửi cho từng site để rà soát và cập nhật lại tồn kho tham khảo
 * cho một mặt hàng mới vừa được thêm vào danh mục.
 */
public class InventoryUpdateRequest {

    public enum Status {
        PENDING,
        COMPLETED
    }

    private int id;
    private String merchandiseCode;
    private String siteCode;
    private Status status;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMerchandiseCode() { return merchandiseCode; }
    public void setMerchandiseCode(String merchandiseCode) { this.merchandiseCode = merchandiseCode; }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
