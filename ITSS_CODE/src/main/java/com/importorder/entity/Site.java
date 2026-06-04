package com.importorder.entity;

import java.time.LocalDateTime;

/**
 * Site nhập khẩu ở nước ngoài.
 */
public class Site {

    private String code;
    private String name;
    private int deliveryDaysByShip;  // Số ngày vận chuyển bằng tàu
    private int deliveryDaysByAir;   // Số ngày vận chuyển bằng hàng không
    private String note;
    private LocalDateTime createdAt;

    public Site() {}

    public Site(String code, String name, int deliveryDaysByShip, int deliveryDaysByAir,
                String note, LocalDateTime createdAt) {
        this.code = code;
        this.name = name;
        this.deliveryDaysByShip = deliveryDaysByShip;
        this.deliveryDaysByAir = deliveryDaysByAir;
        this.note = note;
        this.createdAt = createdAt;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDeliveryDaysByShip() { return deliveryDaysByShip; }
    public void setDeliveryDaysByShip(int deliveryDaysByShip) { this.deliveryDaysByShip = deliveryDaysByShip; }

    public int getDeliveryDaysByAir() { return deliveryDaysByAir; }
    public void setDeliveryDaysByAir(int deliveryDaysByAir) { this.deliveryDaysByAir = deliveryDaysByAir; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Site{code='" + code + "', name='" + name + "'}";
    }
}