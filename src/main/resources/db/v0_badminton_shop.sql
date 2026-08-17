-- ============================================================
-- badminton_shop DDL v2
-- Thay đổi so với v1:
--   - Bỏ contact_messages (xử lý qua email trực tiếp)
--   - Bỏ signup_verification (chuyển sang Redis)
--   - addresses/orders dùng GHN administrative ID (province/district/ward)
--     thay vì text tự do, bỏ country (chỉ bán trong nước)
--   - Thêm brand, category có phân cấp (parent_id) + slug
--   - Thêm product_attributes (EAV) cho thuộc tính kỹ thuật riêng từng loại hàng
--   - product_variants.sku UNIQUE
--   - orders có address_id (nullable, trace) + shipping_* copy tại thời điểm đặt hàng
--   - users.locked/enabled gộp thành status ENUM
-- ============================================================

-- Xóa DB cũ nếu có, tạo mới, set charset chuẩn cho tiếng Việt
DROP DATABASE IF EXISTS badminton_shop;
CREATE DATABASE badminton_shop
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE badminton_shop;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. ROLE
-- ============================================================
CREATE TABLE IF NOT EXISTS role (
                                    id BIGINT NOT NULL AUTO_INCREMENT,
                                    create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    code VARCHAR(50) NOT NULL,        -- 'ADMIN', 'CUSTOMER', 'STAFF'...
    name VARCHAR(100) NOT NULL,       -- tên hiển thị, vd 'Quản trị viên'
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT NOT NULL AUTO_INCREMENT,
                                     create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role_id BIGINT NOT NULL,
    status ENUM('ACTIVE','INACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role_id (role_id),
    CONSTRAINT fk_users_role
    FOREIGN KEY (role_id) REFERENCES role (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. ĐỊA GIỚI HÀNH CHÍNH (sync định kỳ từ GHN API)
-- ============================================================
CREATE TABLE IF NOT EXISTS provinces (
                                         id INT NOT NULL,                 -- ProvinceID từ GHN
                                         name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS districts (
                                         id INT NOT NULL,                 -- DistrictID từ GHN
                                         province_id INT NOT NULL,
                                         name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_districts_province (province_id),
    CONSTRAINT fk_districts_province
    FOREIGN KEY (province_id) REFERENCES provinces (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wards (
                                     code VARCHAR(20) NOT NULL,       -- WardCode từ GHN (dạng string)
    district_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (code),
    KEY idx_wards_district (district_id),
    CONSTRAINT fk_wards_district
    FOREIGN KEY (district_id) REFERENCES districts (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. ADDRESSES (sổ địa chỉ user)
-- ============================================================
CREATE TABLE IF NOT EXISTS addresses (
                                         id BIGINT NOT NULL AUTO_INCREMENT,
                                         create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    line1 VARCHAR(255) NOT NULL,          -- số nhà, tên đường
    province_id INT NOT NULL,
    district_id INT NOT NULL,
    ward_code VARCHAR(20) NOT NULL,
    primary_address ENUM('PRIMARY','SECONDARY') NOT NULL DEFAULT 'SECONDARY',
    PRIMARY KEY (id),
    KEY idx_addresses_user_id (user_id),
    KEY idx_addresses_province (province_id),
    KEY idx_addresses_district (district_id),
    KEY idx_addresses_ward (ward_code),
    CONSTRAINT fk_addresses_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE,
    CONSTRAINT fk_addresses_province
    FOREIGN KEY (province_id) REFERENCES provinces (id),
    CONSTRAINT fk_addresses_district
    FOREIGN KEY (district_id) REFERENCES districts (id),
    CONSTRAINT fk_addresses_ward
    FOREIGN KEY (ward_code) REFERENCES wards (code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. BRAND
-- ============================================================
CREATE TABLE IF NOT EXISTS brand (
                                     id BIGINT NOT NULL AUTO_INCREMENT,
                                     create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    logo_url VARCHAR(1024) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_brand_slug (slug)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. CATEGORY (có phân cấp cha/con)
-- ============================================================
CREATE TABLE IF NOT EXISTS category (
                                        id BIGINT NOT NULL AUTO_INCREMENT,
                                        create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    parent_id BIGINT NULL,
    display_order INT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_slug (slug),
    KEY idx_category_parent (parent_id),
    CONSTRAINT fk_category_parent
    FOREIGN KEY (parent_id) REFERENCES category (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 7. PRODUCTS
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
                                        id BIGINT NOT NULL AUTO_INCREMENT,
                                        create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,            -- RACKET, SHOES, APPAREL, ACCESSORY... (validate ở Java enum)
    category_id BIGINT NOT NULL,
    brand_id BIGINT NOT NULL,
    product_status VARCHAR(20) NOT NULL,
    price DECIMAL(19,2) NOT NULL,
    sale_price DECIMAL(19,2) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_products_slug (slug),
    KEY idx_products_category_id (category_id),
    KEY idx_products_brand_id (brand_id),
    KEY idx_products_status (product_status),
    KEY idx_products_category_status_price (category_id, product_status, price),
    CONSTRAINT fk_products_category
    FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_products_brand
    FOREIGN KEY (brand_id) REFERENCES brand (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 8. PRODUCT ATTRIBUTES (EAV - thuộc tính kỹ thuật riêng từng loại hàng)
--    vd: weight=4U, grip_size=G5, flex=Medium (vợt)
--        shoe_tech=Power Cushion (giày)
--        material=Polyester (quần áo)
-- ============================================================
CREATE TABLE IF NOT EXISTS product_attributes (
                                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                                  product_id BIGINT NOT NULL,
                                                  attr_name VARCHAR(100) NOT NULL,
    attr_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_product_attributes_product (product_id),
    KEY idx_product_attributes_name (attr_name),
    CONSTRAINT fk_product_attributes_product
    FOREIGN KEY (product_id) REFERENCES products (id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 9. PRODUCT COLORS
-- ============================================================
CREATE TABLE IF NOT EXISTS product_colors (
                                              id BIGINT NOT NULL AUTO_INCREMENT,
                                              create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    color_name VARCHAR(100) NOT NULL,
    hex_code VARCHAR(20) NULL,
    display_order INT NULL DEFAULT 0,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_product_colors_product_id (product_id),
    CONSTRAINT fk_product_colors_product
    FOREIGN KEY (product_id) REFERENCES products (id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 10. PRODUCT IMAGES
-- ============================================================
CREATE TABLE IF NOT EXISTS product_images (
                                              id BIGINT NOT NULL AUTO_INCREMENT,
                                              create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    url VARCHAR(1024) NOT NULL,
    provider_public_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NULL,
    alt_text VARCHAR(512) NULL,
    is_main ENUM('MAIN','SECONDARY') NULL DEFAULT 'SECONDARY',
    order_index INT NULL DEFAULT 0,
    color_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_product_images_color_id (color_id),
    CONSTRAINT fk_product_images_color
    FOREIGN KEY (color_id) REFERENCES product_colors (id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11. PRODUCT VARIANTS
-- ============================================================
CREATE TABLE IF NOT EXISTS product_variants (
                                                id BIGINT NOT NULL AUTO_INCREMENT,
                                                create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    sku VARCHAR(100) NOT NULL,
    size VARCHAR(50) NULL,               -- '40', '41' (giày) | '3U','4U' (vợt) | 'S','M','L' (áo)
    stock INT NOT NULL DEFAULT 0,
    active ENUM('ACTIVE','INACTIVE') NULL DEFAULT 'ACTIVE',
    inventory_status VARCHAR(20) NULL DEFAULT 'IN_ORDER',
    color_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_variants_sku (sku),
    UNIQUE KEY uk_product_variants_color_size (color_id, size),
    KEY idx_product_variants_color_id (color_id),
    CONSTRAINT fk_product_variants_color
    FOREIGN KEY (color_id) REFERENCES product_colors (id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 12. CART ITEMS
-- ============================================================
CREATE TABLE IF NOT EXISTS cart_items (
                                          id BIGINT NOT NULL AUTO_INCREMENT,
                                          create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_variant (user_id, variant_id),
    KEY idx_cart_items_variant_id (variant_id),
    CONSTRAINT fk_cart_items_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_variant
    FOREIGN KEY (variant_id) REFERENCES product_variants (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 13. ORDERS
--     shipping_* = snapshot copy từ addresses tại thời điểm đặt hàng, tự chứa
--     đủ thông tin giao hàng, không cần trace ngược về addresses.
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
                                      id BIGINT NOT NULL AUTO_INCREMENT,
                                      create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    shipping_method VARCHAR(15) NOT NULL DEFAULT 'STANDARD',
    shipping_recipient_name VARCHAR(255) NOT NULL,
    shipping_phone VARCHAR(20) NOT NULL,
    shipping_line1 VARCHAR(255) NOT NULL,
    shipping_province_id INT NOT NULL,
    shipping_district_id INT NOT NULL,
    shipping_ward_code VARCHAR(20) NOT NULL,
    subtotal DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    shipping_cost DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_orders_user_id (user_id),
    KEY idx_orders_status (order_status),
    CONSTRAINT fk_orders_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 14. ORDER ITEMS
-- ============================================================
CREATE TABLE IF NOT EXISTS order_items (
                                           id BIGINT NOT NULL AUTO_INCREMENT,
                                           create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT NULL,
    sku VARCHAR(100) NULL,
    product_name VARCHAR(255) NOT NULL,
    size VARCHAR(20) NULL,
    color VARCHAR(50) NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order_id (order_id),
    KEY idx_order_items_product_id (product_id),
    KEY idx_order_items_variant_id (variant_id),
    CONSTRAINT fk_order_items_order
    FOREIGN KEY (order_id) REFERENCES orders (id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 15. PAYMENT TRANSACTION
-- ============================================================
CREATE TABLE IF NOT EXISTS payment_transaction (
                                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                                   create_date DATETIME(6) NULL,
    update_date DATETIME(6) NULL,
    order_id BIGINT NOT NULL,
    provider VARCHAR(255) NOT NULL,
    txn_ref VARCHAR(100) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    response_code VARCHAR(10) NULL,
    transaction_status VARCHAR(10) NULL,
    transaction_no VARCHAR(20) NULL,
    bank_code VARCHAR(20) NULL,
    pay_date DATETIME(6) NULL,
    expire_date DATETIME(6) NULL,
    ip_address VARCHAR(50) NULL,
    ipn_processed ENUM('PROCESSED','PENDING') NOT NULL DEFAULT 'PENDING',
    raw_request_payload LONGTEXT NULL,
    raw_response_payload LONGTEXT NULL,
    failure_reason VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_txn_ref (txn_ref),
    KEY idx_order_id (order_id),
    KEY idx_payment_status (status),
    KEY idx_payment_order_status (order_id, status),
    CONSTRAINT fk_payment_transaction_order
    FOREIGN KEY (order_id) REFERENCES orders (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- Xong. 14 bảng đã được tạo trong database badminton_shop.