package com.importorder.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Đơn đặt hàng gửi tới một Site cụ thể, được tạo từ một SaleRequest.
 */
public class SiteOrder {

    public enum Status {
        ORDER_RECEIVED,         // Site đã nhận đơn đặt hàng
        IN_TRANSIT,             // Đang vận chuyển
        DELIVERED,              // Vận chuyển thành công
        SHORTAGE_REPORTED,      // Kho báo thiếu hàng theo từng mặt hàng
        WAREHOUSE_CONFIRMED,    // Kiểm hàng thành công (quản lý kho xác nhận)
        CANCELLED               // Đã hủy
    }

    public enum DeliveryMeans {
        SHIP_DELIVERY,
        AIR_DELIVERY
    }

    private int id;
    private int saleRequestId;          // FK -> SaleRequest.id
    private String siteCode;            // FK -> Site.code
    private Status status;
    private DeliveryMeans deliveryMeans;
    private LocalDate estimatedDeliveryDate;  // Ngày dự kiến nhận hàng
    private LocalDate actualDeliveryDate;     // Ngày thực tế nhận hàng (quản lý kho cập nhật)
    private LocalDateTime createdAt;
    private int createdBy;              // FK -> User.id (role = OVERSEAS_ORDER_DEPT)
    private String cancelReason;

    public SiteOrder() {}

    public SiteOrder(int id, int saleRequestId, String siteCode, Status status,
                     DeliveryMeans deliveryMeans, LocalDate estimatedDeliveryDate,
                     LocalDate actualDeliveryDate, LocalDateTime createdAt, int createdBy) {
        this.id = id;
        this.saleRequestId = saleRequestId;
        this.siteCode = siteCode;
        this.status = status;
        this.deliveryMeans = deliveryMeans;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.actualDeliveryDate = actualDeliveryDate;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSaleRequestId() { return saleRequestId; }
    public void setSaleRequestId(int saleRequestId) { this.saleRequestId = saleRequestId; }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public DeliveryMeans getDeliveryMeans() { return deliveryMeans; }
    public void setDeliveryMeans(DeliveryMeans deliveryMeans) { this.deliveryMeans = deliveryMeans; }

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

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    @Override
    public String toString() {
        return "SiteOrder{id=" + id + ", siteCode='" + siteCode + "', status=" + status
                + ", deliveryMeans=" + deliveryMeans + "}";
    }
}
