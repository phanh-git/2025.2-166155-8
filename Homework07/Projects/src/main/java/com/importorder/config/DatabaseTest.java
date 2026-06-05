package com.importorder.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class để test kết nối và schema PostgreSQL
 */
public class DatabaseTest {
    
    public static void main(String[] args) {
        System.out.println("=== Test Kết Nối PostgreSQL ===\n");
        
        // 1. Test kết nối cơ bản
        testConnection();
        
        // 2. Kiểm tra database info
        testDatabaseInfo();
        
        // 3. Kiểm tra các bảng
        testTables();
    }
    
    private static void testConnection() {
        System.out.println("1️⃣ Test kết nối cơ bản...");
        try {
            if (DatabaseConnection.testConnection()) {
                System.out.println("✅ Kết nối thành công!\n");
            } else {
                System.out.println("❌ Kết nối thất bại!\n");
            }
        } catch (Exception e) {
            System.out.println("❌ Lỗi: " + e.getMessage() + "\n");
        }
    }
    
    private static void testDatabaseInfo() {
        System.out.println("2️⃣ Thông tin database...");
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();
            System.out.println("   Driver: " + metadata.getDriverName());
            System.out.println("   URL: " + metadata.getURL());
            System.out.println("   Database: " + metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion());
            System.out.println("   ✅ OK\n");
        } catch (SQLException e) {
            System.out.println("   ❌ Lỗi: " + e.getMessage() + "\n");
        }
    }
    
    private static void testTables() {
        System.out.println("3️⃣ Kiểm tra các bảng...");
        String[] tables = {
            "users",
            "merchandises",
            "sites", 
            "site_users",
            "site_merchandises",
            "sale_requests",
            "sale_request_items",
            "site_orders",
            "site_order_items"
        };
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet tables_rs = metadata.getTables(null, "public", "%", new String[]{"TABLE"});
            
            java.util.Set<String> existingTables = new java.util.HashSet<>();
            while (tables_rs.next()) {
                existingTables.add(tables_rs.getString("TABLE_NAME"));
            }
            
            for (String table : tables) {
                if (existingTables.contains(table)) {
                    countRows(conn, table);
                } else {
                    System.out.println("   ❌ " + table + " không tồn tại");
                }
            }
            System.out.println("   ✅ OK\n");
        } catch (SQLException e) {
            System.out.println("   ❌ Lỗi: " + e.getMessage() + "\n");
        }
    }
    
    private static void countRows(Connection conn, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM " + tableName)) {
            if (rs.next()) {
                int count = rs.getInt("cnt");
                System.out.println("   ✅ " + tableName + " (" + count + " rows)");
            }
        }
    }
}
