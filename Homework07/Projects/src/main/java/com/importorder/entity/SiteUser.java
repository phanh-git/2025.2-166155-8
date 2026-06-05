package com.importorder.entity;

/**
 * Liên kết User có role SITE_USER với một Site cụ thể.
 * Admin tạo user, sau đó gán site qua bảng này.
 */
public class SiteUser {

    private int userId;     // FK -> User.id (role = SITE_USER)
    private String siteCode; // FK -> Site.code

    public SiteUser() {}

    public SiteUser(int userId, String siteCode) {
        this.userId = userId;
        this.siteCode = siteCode;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    @Override
    public String toString() {
        return "SiteUser{userId=" + userId + ", siteCode='" + siteCode + "'}";
    }
}