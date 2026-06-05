package com.importorder.entity;

import java.time.LocalDateTime;

public class User {

    public enum Role {
        ADMIN,
        WAREHOUSE_MANAGER,      // Quản lý kho
        SALES_DEPARTMENT,       // Bộ phận bán hàng
        OVERSEAS_ORDER_DEPT,    // Bộ phận đặt hàng quốc tế
        SITE_USER               // Người dùng của bên site
    }

    private int id;
    private String username;
    private String password;
    private Role role;
    private LocalDateTime createdAt;

    public User() {}

    public User(int id, String username, String password, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + "}";
    }
}