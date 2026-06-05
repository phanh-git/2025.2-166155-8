package com.importorder.entity;

/**
 * Số lượng hàng trong kho tại từng Site.
 * Site tự cập nhật thông tin này trên hệ thống.
 */
public class SiteMerchandise {

    private String siteCode;         // FK -> Site.code
    private String merchandiseCode;  // FK -> Merchandise.code
    private int quantity;            // Số lượng trong kho tại site
    private String unit;

    public SiteMerchandise() {}

    public SiteMerchandise(String siteCode, String merchandiseCode, int quantity, String unit) {
        this.siteCode = siteCode;
        this.merchandiseCode = merchandiseCode;
        this.quantity = quantity;
        this.unit = unit;
    }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public String getMerchandiseCode() { return merchandiseCode; }
    public void setMerchandiseCode(String merchandiseCode) { this.merchandiseCode = merchandiseCode; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    @Override
    public String toString() {
        return "SiteMerchandise{siteCode='" + siteCode + "', merchandiseCode='" + merchandiseCode
                + "', quantity=" + quantity + "}";
    }
}