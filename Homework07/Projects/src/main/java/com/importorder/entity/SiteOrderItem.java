package com.importorder.entity;

/**
 * Từng mặt hàng trong một đơn đặt hàng gửi tới Site.
 * Liên kết ngược về SaleRequestItem để truy vết mặt hàng nào của yêu cầu ban đầu
 * được xử lý bởi đơn site nào (đặc biệt khi 1 mặt hàng được chia cho nhiều site).
 */
public class SiteOrderItem {

    private int id;
    private int siteOrderId;            // FK -> SiteOrder.id
    private int saleRequestItemId;      // FK -> SaleRequestItem.id (để truy vết)
    private String merchandiseCode;     // FK -> Merchandise.code
    private int quantity;
    private String unit;
    private Integer receivedQuantity;
    private String shortageNote;

    public SiteOrderItem() {}

    public SiteOrderItem(int id, int siteOrderId, int saleRequestItemId,
                         String merchandiseCode, int quantity, String unit) {
        this.id = id;
        this.siteOrderId = siteOrderId;
        this.saleRequestItemId = saleRequestItemId;
        this.merchandiseCode = merchandiseCode;
        this.quantity = quantity;
        this.unit = unit;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSiteOrderId() { return siteOrderId; }
    public void setSiteOrderId(int siteOrderId) { this.siteOrderId = siteOrderId; }

    public int getSaleRequestItemId() { return saleRequestItemId; }
    public void setSaleRequestItemId(int saleRequestItemId) {
        this.saleRequestItemId = saleRequestItemId;
    }

    public String getMerchandiseCode() { return merchandiseCode; }
    public void setMerchandiseCode(String merchandiseCode) { this.merchandiseCode = merchandiseCode; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }

    public String getShortageNote() { return shortageNote; }
    public void setShortageNote(String shortageNote) { this.shortageNote = shortageNote; }

    @Override
    public String toString() {
        return "SiteOrderItem{id=" + id + ", siteOrderId=" + siteOrderId
                + ", merchandiseCode='" + merchandiseCode + "', quantity=" + quantity + "}";
    }
}
