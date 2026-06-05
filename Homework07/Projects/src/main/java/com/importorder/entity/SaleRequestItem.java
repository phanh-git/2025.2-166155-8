package com.importorder.entity;

import java.time.LocalDate;

/**
 * Từng mặt hàng trong một yêu cầu đặt hàng của Bộ phận bán hàng.
 */
public class SaleRequestItem {

    private int id;
    private int saleRequestId;       // FK -> SaleRequest.id
    private String merchandiseCode;  // FK -> Merchandise.code
    private int quantityOrdered;
    private String unit;
    private LocalDate desiredDeliveryDate;

    public SaleRequestItem() {}

    public SaleRequestItem(int id, int saleRequestId, String merchandiseCode,
                           int quantityOrdered, String unit, LocalDate desiredDeliveryDate) {
        this.id = id;
        this.saleRequestId = saleRequestId;
        this.merchandiseCode = merchandiseCode;
        this.quantityOrdered = quantityOrdered;
        this.unit = unit;
        this.desiredDeliveryDate = desiredDeliveryDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSaleRequestId() { return saleRequestId; }
    public void setSaleRequestId(int saleRequestId) { this.saleRequestId = saleRequestId; }

    public String getMerchandiseCode() { return merchandiseCode; }
    public void setMerchandiseCode(String merchandiseCode) { this.merchandiseCode = merchandiseCode; }

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
        return "SaleRequestItem{id=" + id + ", merchandiseCode='" + merchandiseCode
                + "', quantityOrdered=" + quantityOrdered + ", desiredDeliveryDate=" + desiredDeliveryDate + "}";
    }
}