package com.importorder.dto;

import com.importorder.entity.InventoryUpdateRequest;

import java.time.LocalDateTime;
public class InventoryUpdateRequestDTO {

    private int id;
    private String merchandiseCode;
    private String merchandiseName;
    private String merchandiseUnit;
    private String siteCode;
    private String siteName;
    private InventoryUpdateRequest.Status status;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMerchandiseCode() { return merchandiseCode; }
    public void setMerchandiseCode(String merchandiseCode) { this.merchandiseCode = merchandiseCode; }

    public String getMerchandiseName() { return merchandiseName; }
    public void setMerchandiseName(String merchandiseName) { this.merchandiseName = merchandiseName; }

    public String getMerchandiseUnit() { return merchandiseUnit; }
    public void setMerchandiseUnit(String merchandiseUnit) { this.merchandiseUnit = merchandiseUnit; }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public InventoryUpdateRequest.Status getStatus() { return status; }
    public void setStatus(InventoryUpdateRequest.Status status) { this.status = status; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
