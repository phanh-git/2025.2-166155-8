package com.importorder.dto;

import com.importorder.entity.SiteOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO đại diện cho một đơn hàng gửi tới site (header + items).
 */
public class SiteOrderDTO {

    private int id;
    private int saleRequestId;
    private String siteCode;
    private String siteName;            // join để hiển thị
    private SiteOrder.Status status;
    private SiteOrder.DeliveryMeans deliveryMeans;
    private LocalDate estimatedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private LocalDateTime createdAt;
    private int createdBy;
    private String createdByUsername;
    private String cancelReason;
    private List<SiteOrderItemDTO> items;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public SiteOrderDTO() {}

    // ─── Getters / Setters ──────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSaleRequestId() { return saleRequestId; }
    public void setSaleRequestId(int saleRequestId) { this.saleRequestId = saleRequestId; }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public SiteOrder.Status getStatus() { return status; }
    public void setStatus(SiteOrder.Status status) { this.status = status; }

    public SiteOrder.DeliveryMeans getDeliveryMeans() { return deliveryMeans; }
    public void setDeliveryMeans(SiteOrder.DeliveryMeans deliveryMeans) {
        this.deliveryMeans = deliveryMeans;
    }

    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public LocalDate getActualDeliveryDate() { return actualDeliveryDate; }
    public void setActualDeliveryDate(LocalDate actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public List<SiteOrderItemDTO> getItems() { return items; }
    public void setItems(List<SiteOrderItemDTO> items) { this.items = items; }
}
