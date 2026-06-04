package com.importorder.dto;

import java.time.LocalDate;

/**
 * Một dòng mặt hàng trong yêu cầu đặt hàng.
 */
public class SaleRequestItemDTO {

    private int id;
    private int saleRequestId;
    private String merchandiseCode;
    private String merchandiseName;     // join từ bảng merchandises để hiển thị
    private int quantityOrdered;
    private String unit;
    private LocalDate desiredDeliveryDate;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public SaleRequestItemDTO() {}

    // ─── Getters / Setters ──────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSaleRequestId() { return saleRequestId; }
    public void setSaleRequestId(int saleRequestId) { this.saleRequestId = saleRequestId; }

    public String getMerchandiseCode() { return merchandiseCode; }
    public void setMerchandiseCode(String merchandiseCode) { this.merchandiseCode = merchandiseCode; }

    public String getMerchandiseName() { return merchandiseName; }
    public void setMerchandiseName(String merchandiseName) { this.merchandiseName = merchandiseName; }

    public int getQuantityOrdered() { return quantityOrdered; }
    public void setQuantityOrdered(int quantityOrdered) { this.quantityOrdered = quantityOrdered; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public LocalDate getDesiredDeliveryDate() { return desiredDeliveryDate; }
    public void setDesiredDeliveryDate(LocalDate desiredDeliveryDate) {
        this.desiredDeliveryDate = desiredDeliveryDate;
    }

    @Override
    public String toString() {
        return "SaleRequestItemDTO{merchandiseCode='" + merchandiseCode
                + "', qty=" + quantityOrdered + ", desiredDate=" + desiredDeliveryDate + "}";
    }
}