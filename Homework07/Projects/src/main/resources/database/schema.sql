-- Schema mới theo bộ entity refactor
-- Chạy trên database trống sau khi DROP/CREATE lại DB

-- =========================
-- USERS
-- =========================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- MERCHANDISES
-- =========================
CREATE TABLE IF NOT EXISTS merchandises (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(50)
);

-- =========================
-- SITES
-- =========================
CREATE TABLE IF NOT EXISTS sites (
    code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    delivery_days_by_ship INT NOT NULL DEFAULT 0,
    delivery_days_by_air INT NOT NULL DEFAULT 0,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- SITE_USERS
-- =========================
CREATE TABLE IF NOT EXISTS site_users (
    user_id BIGINT PRIMARY KEY,
    site_code VARCHAR(50) NOT NULL,
    CONSTRAINT fk_site_users_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_site_users_site
        FOREIGN KEY (site_code) REFERENCES sites(code)
        ON DELETE CASCADE
);

-- =========================
-- SITE_MERCHANDISES
-- =========================
CREATE TABLE IF NOT EXISTS site_merchandises (
    site_code VARCHAR(50) NOT NULL,
    merchandise_code VARCHAR(50) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    unit VARCHAR(50),
    PRIMARY KEY (site_code, merchandise_code),
    CONSTRAINT fk_site_merchandises_site
        FOREIGN KEY (site_code) REFERENCES sites(code)
        ON DELETE CASCADE,
    CONSTRAINT fk_site_merchandises_merchandise
        FOREIGN KEY (merchandise_code) REFERENCES merchandises(code)
        ON DELETE CASCADE
);

-- =========================
-- INVENTORY_UPDATE_REQUESTS
-- =========================
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
);

-- =========================
-- SALE_REQUESTS
-- =========================
CREATE TABLE IF NOT EXISTS sale_requests (
    id BIGSERIAL PRIMARY KEY,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
    cancel_requested BOOLEAN NOT NULL DEFAULT FALSE,
    cancel_request_reason TEXT,
    cancel_resolution_note TEXT,
    CONSTRAINT fk_sale_requests_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE RESTRICT
);

-- =========================
-- SALE_REQUEST_ITEMS
-- =========================
CREATE TABLE IF NOT EXISTS sale_request_items (
    id BIGSERIAL PRIMARY KEY,
    sale_request_id BIGINT NOT NULL,
    merchandise_code VARCHAR(50) NOT NULL,
    quantity_ordered INT NOT NULL,
    unit VARCHAR(50),
    desired_delivery_date DATE,
    CONSTRAINT fk_sale_request_items_request
        FOREIGN KEY (sale_request_id) REFERENCES sale_requests(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_sale_request_items_merchandise
        FOREIGN KEY (merchandise_code) REFERENCES merchandises(code)
        ON DELETE RESTRICT
);

-- =========================
-- SITE_ORDERS
-- =========================
CREATE TABLE IF NOT EXISTS site_orders (
    id BIGSERIAL PRIMARY KEY,
    sale_request_id BIGINT NOT NULL,
    site_code VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ORDER_RECEIVED',
    delivery_means VARCHAR(50) NOT NULL,
    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    cancel_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    CONSTRAINT fk_site_orders_request
        FOREIGN KEY (sale_request_id) REFERENCES sale_requests(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_site_orders_site
        FOREIGN KEY (site_code) REFERENCES sites(code)
        ON DELETE RESTRICT,
    CONSTRAINT fk_site_orders_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE RESTRICT
);

-- =========================
-- SITE_ORDER_ITEMS
-- =========================
CREATE TABLE IF NOT EXISTS site_order_items (
    id BIGSERIAL PRIMARY KEY,
    site_order_id BIGINT NOT NULL,
    sale_request_item_id BIGINT NOT NULL,
    merchandise_code VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    unit VARCHAR(50),
    received_quantity INT,
    shortage_note TEXT,
    CONSTRAINT fk_site_order_items_order
        FOREIGN KEY (site_order_id) REFERENCES site_orders(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_site_order_items_request_item
        FOREIGN KEY (sale_request_item_id) REFERENCES sale_request_items(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_site_order_items_merchandise
        FOREIGN KEY (merchandise_code) REFERENCES merchandises(code)
        ON DELETE RESTRICT
);

-- =========================
-- SEED DATA
-- =========================
INSERT INTO users (username, password, role, created_at) VALUES
('admin', 'admin', 'ADMIN', CURRENT_TIMESTAMP),
('warehouse', 'warehouse', 'WAREHOUSE_MANAGER', CURRENT_TIMESTAMP),
('sales', 'sales', 'SALES_DEPARTMENT', CURRENT_TIMESTAMP),
('overseas', 'overseas', 'OVERSEAS_ORDER_DEPT', CURRENT_TIMESTAMP),
('site_jp', 'site_jp', 'SITE_USER', CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

INSERT INTO merchandises (code, name, unit) VALUES
('P001', 'Sữa bột nhập khẩu', 'hộp'),
('P002', 'Bánh quy', 'thùng'),
('P003', 'Dầu olive', 'chai')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sites (code, name, delivery_days_by_ship, delivery_days_by_air, note, created_at) VALUES
('JP01', 'Tokyo Import Site', 20, 5, 'Japan', CURRENT_TIMESTAMP),
('KR01', 'Seoul Import Site', 16, 4, 'Korea', CURRENT_TIMESTAMP),
('SG01', 'Singapore Hub', 10, 3, 'Singapore', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO site_merchandises (site_code, merchandise_code, quantity, unit) VALUES
('JP01', 'P001', 80, 'hộp'),
('JP01', 'P002', 100, 'thùng'),
('KR01', 'P001', 60, 'hộp'),
('KR01', 'P003', 90, 'chai'),
('SG01', 'P001', 30, 'hộp'),
('SG01', 'P002', 45, 'thùng'),
('SG01', 'P003', 20, 'chai')
ON CONFLICT (site_code, merchandise_code) DO NOTHING;

INSERT INTO site_users (user_id, site_code)
SELECT u.id, 'JP01'
FROM users u
WHERE u.username = 'site_jp'
ON CONFLICT (user_id) DO UPDATE SET site_code = EXCLUDED.site_code;
