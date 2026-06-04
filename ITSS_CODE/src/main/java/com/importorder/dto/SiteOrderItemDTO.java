package com.importorder.dto;

/**
 * Một dòng mặt hàng trong đơn hàng gửi tới site.
 */
public class SiteOrderItemDTO {

    private int id;
    private int siteOrderId;
    private int saleRequestItemId;
    private String merchandiseCode;
    private String merchandiseName;
    private int quantity;
    private String unit;
    private Integer receivedQuantity;
    private String shortageNote;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public SiteOrderItemDTO() {}

    // ─── Getters / Setters ──────────────────────────────────────────────────────

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

    public String getMerchandiseName() { return merchandiseName; }
    public void setMerchandiseName(String merchandiseName) { this.merchandiseName = merchandiseName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }

    public String getShortageNote() { return shortageNote; }
    public void setShortageNote(String shortageNote) { this.shortageNote = shortageNote; }
}
