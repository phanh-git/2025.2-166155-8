package com.importorder.entity;

/**
 * Danh mục hàng hóa (không chứa số lượng tồn kho - thuộc hệ thống quản lý kho).
 */
public class Merchandise {

    private int id;
    private String code;    // Mã hàng, 6 chữ số, random
    private String name;
    private String unit;    // Đơn vị (có thể null)

    public Merchandise() {}


    public Merchandise(int id, String code, String name, String unit) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.unit = unit;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    @Override
    public String toString() {
        return "Merchandise{code='" + code + "', name='" + name + "', unit='" + unit + "'}";
    }
}