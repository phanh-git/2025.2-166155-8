package com.importorder.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class để quản lý kết nối PostgreSQL
 * Đọc cấu hình từ application.properties
 */
public class DatabaseConnection {
    
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    static {
        loadProperties();
    }

    /**
     * Tải cấu hình từ application.properties
     */
    private static void loadProperties() {
        Properties props = new Properties();
        try (InputStream input = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            
            if (input == null) {
                System.err.println("application.properties không tìm thấy, dùng giá trị mặc định");
                DB_URL = "jdbc:postgresql://localhost:5432/importorder_db";
                DB_USER = "postgres";
                DB_PASSWORD = "postgres";
                return;
            }
            
            props.load(input);
            DB_URL = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/importorder_db");
            DB_USER = props.getProperty("db.user", "postgres");
            DB_PASSWORD = props.getProperty("db.password", "postgres");
            
            System.out.println("✓ Đã tải cấu hình database từ application.properties");
        } catch (IOException e) {
            System.err.println("Lỗi đọc application.properties: " + e.getMessage());
            DB_URL = "jdbc:postgresql://localhost:5432/importorder_db";
            DB_USER = "postgres";
            DB_PASSWORD = "postgres";
        }
    }

    static {
        try {
            Class.forName("org.postgresql.Driver");
            ensureSchemaCompatibility();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL Driver không tìm thấy", e);
        }
    }

    /**
     * Lấy kết nối mới đến PostgreSQL
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Test kết nối database
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối database: " + e.getMessage());
            return false;
        }
    }

    private static void ensureSchemaCompatibility() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    ALTER TABLE IF EXISTS sale_requests
                    ADD COLUMN IF NOT EXISTS cancel_requested BOOLEAN NOT NULL DEFAULT FALSE
                    """);
            stmt.execute("""
                    ALTER TABLE IF EXISTS sale_requests
                    ADD COLUMN IF NOT EXISTS cancel_request_reason TEXT
                    """);
            stmt.execute("""
                    ALTER TABLE IF EXISTS sale_requests
                    ADD COLUMN IF NOT EXISTS cancel_resolution_note TEXT
                    """);
            stmt.execute("""
                    ALTER TABLE IF EXISTS site_orders
                    ADD COLUMN IF NOT EXISTS cancel_reason TEXT
                    """);
            stmt.execute("""
                    ALTER TABLE IF EXISTS site_order_items
                    ADD COLUMN IF NOT EXISTS received_quantity INT
                    """);
            stmt.execute("""
                    ALTER TABLE IF EXISTS site_order_items
                    ADD COLUMN IF NOT EXISTS shortage_note TEXT
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS merchandise_inventory_update_requests (
                        id BIGSERIAL PRIMARY KEY,
                        merchandise_code VARCHAR(50) NOT NULL,
                        site_code VARCHAR(50) NOT NULL,
                        status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                        requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        responded_at TIMESTAMP,
                        CONSTRAINT fk_merch_inventory_update_requests_merchandise
                            FOREIGN KEY (merchandise_code) REFERENCES merchandises(code)
                            ON DELETE CASCADE,
                        CONSTRAINT fk_merch_inventory_update_requests_site
                            FOREIGN KEY (site_code) REFERENCES sites(code)
                            ON DELETE CASCADE,
                        CONSTRAINT uq_merch_inventory_update_request_per_site
                            UNIQUE (merchandise_code, site_code)
                    )
                    """);
        } catch (SQLException e) {
            System.err.println("Không thể đồng bộ schema bổ sung: " + e.getMessage());
        }
    }
}
