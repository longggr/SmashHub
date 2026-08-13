DROP DATABASE IF EXISTS badminton_shop;

CREATE DATABASE badminton_shop
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE badminton_shop;

-- =========================================================
-- 1. USERS
-- Thêm: phone, email_verified_at (dùng chung với signup_verification)
-- Giữ nguyên role ENUM theo yêu cầu.
-- =========================================================
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    update_date DATETIME(6),

    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(30),

    email_verified_at DATETIME(6),

    role ENUM(
        'CUSTOMER',
        'ADMIN'
    ) NOT NULL DEFAULT 'CUSTOMER',

    status ENUM(
        'ACTIVE',
        'LOCKED',
        'DISABLED'
    ) NOT NULL DEFAULT 'ACTIVE',

    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB;

-- =========================================================
-- 2. SIGNUP VERIFICATION
-- =========================================================
CREATE TABLE signup_verification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    update_date DATETIME(6),

    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,

    attempt_count INT NOT NULL DEFAULT 0,
    expires_at DATETIME(6) NOT NULL,
    last_sent_at DATETIME(6),
    used_at DATETIME(6),

    PRIMARY KEY (id),
    KEY idx_signup_verification_email (email)
) ENGINE=InnoDB;

-- =========================================================
-- 3. ADMINISTRATIVE GEOGRAPHY (provinces / districts / wards)
--
-- Đồng bộ trực tiếp từ danh mục hành chính của GHN (Giao Hàng Nhanh):
--   - id của provinces/districts CHÍNH LÀ ProvinceID/DistrictID của GHN.
--   - code của wards CHÍNH LÀ WardCode của GHN (dạng string, không phải số).
-- Tự lưu nội bộ (self-hosting) thay vì gọi API GHN mỗi lần render form địa
-- chỉ, tránh phụ thuộc uptime của bên thứ 3 và giảm độ trễ cho người dùng.
-- Đồng bộ định kỳ (cron / job thủ công) khi GHN cập nhật danh mục.
-- =========================================================
CREATE TABLE provinces (
    id INT NOT NULL,               -- ProvinceID từ GHN, KHÔNG auto-increment
    name VARCHAR(100) NOT NULL,

    PRIMARY KEY (id),
    KEY idx_provinces_name (name)          -- tìm kiếm/autocomplete theo tên Tỉnh/Thành
) ENGINE=InnoDB;

CREATE TABLE districts (
    id INT NOT NULL,               -- DistrictID từ GHN, KHÔNG auto-increment
    province_id INT NOT NULL,

    name VARCHAR(100) NOT NULL,

    PRIMARY KEY (id),

    -- Index kép (province_id, name): vừa phục vụ lọc theo tỉnh (đổ dropdown
    -- Huyện sau khi chọn Tỉnh) VỪA sort theo tên trong cùng 1 lần quét index,
    -- không cần filesort riêng - đúng thứ tự cột theo cách query thực tế dùng.
    KEY idx_districts_province_id_name (province_id, name),

    CONSTRAINT fk_districts_province
        FOREIGN KEY (province_id)
        REFERENCES provinces(id)
) ENGINE=InnoDB;

CREATE TABLE wards (
    code VARCHAR(20) NOT NULL,     -- WardCode từ GHN (string, không phải số)
    district_id INT NOT NULL,

    name VARCHAR(100) NOT NULL,

    PRIMARY KEY (code),

    -- Tương tự districts: phục vụ lọc theo huyện (đổ dropdown Xã) + sort tên
    KEY idx_wards_district_id_name (district_id, name),

    CONSTRAINT fk_wards_district
        FOREIGN KEY (district_id)
        REFERENCES districts(id)
) ENGINE=InnoDB;

-- =========================================================
-- 4. ADDRESSES
--
-- CHUẨN HÓA 3NF: chỉ lưu ID hành chính (province_id/district_id/ward_code)
-- và FK trỏ về bảng danh mục - KHÔNG lưu text tên Tỉnh/Huyện/Xã trực tiếp.
-- Nếu Nhà nước đổi tên hoặc sáp nhập địa giới, chỉ cần cập nhật bảng danh
-- mục, toàn bộ sổ địa chỉ của khách hàng tự động hiển thị tên mới -
-- không cần migrate dữ liệu addresses.
--
-- Đã bỏ "country" (chỉ phục vụ thị trường Việt Nam, mặc định luôn là VN,
-- lưu thêm chỉ tốn chỗ) và "postal_code" (không dùng để định tuyến giao
-- hàng nội địa qua GHN, chỉ làm rối form nhập liệu).
--
-- Lưu ý nghiệp vụ: chỉ 1 địa chỉ DEFAULT / user.
-- MySQL không hỗ trợ partial unique index -> xử lý ở tầng
-- application (transaction: set các default cũ về OTHER
-- trước khi insert/update default mới).
-- =========================================================
CREATE TABLE addresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,

    recipient_name VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,

    -- Gộp thành 1 field duy nhất (thay vì line1/line2) để khớp trực tiếp với
    -- field "to_address" của GHN API khi tạo đơn - GHN chỉ nhận 1 chuỗi địa
    -- chỉ chi tiết duy nhất (số nhà, tên đường...), không có khái niệm nhiều
    -- dòng. Tách line1/line2 chỉ tạo thêm bước nối chuỗi thừa mỗi lần gọi GHN.
    detail_address VARCHAR(255) NOT NULL,

    province_id INT NOT NULL,
    district_id INT NOT NULL,
    ward_code VARCHAR(20) NOT NULL,

    address_type ENUM(
        'DEFAULT',
        'OTHER'
    ) NOT NULL DEFAULT 'OTHER',

    PRIMARY KEY (id),
    KEY idx_addresses_user_id (user_id),
    KEY idx_addresses_province_id (province_id),
    KEY idx_addresses_district_id (district_id),
    KEY idx_addresses_ward_code (ward_code),

    CONSTRAINT fk_addresses_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_addresses_province
        FOREIGN KEY (province_id)
        REFERENCES provinces(id),

    CONSTRAINT fk_addresses_district
        FOREIGN KEY (district_id)
        REFERENCES districts(id),

    CONSTRAINT fk_addresses_ward
        FOREIGN KEY (ward_code)
        REFERENCES wards(code)
) ENGINE=InnoDB;

-- =========================================================
-- 5. CATEGORIES
-- =========================================================
CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,

    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),

    status ENUM(
        'ACTIVE',
        'INACTIVE'
    ) NOT NULL DEFAULT 'ACTIVE',

    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB;

-- =========================================================
-- 6. BRANDS
-- =========================================================
CREATE TABLE brands (
    id BIGINT NOT NULL AUTO_INCREMENT,

    name VARCHAR(100) NOT NULL,
    logo_url VARCHAR(1024),

    status ENUM(
        'ACTIVE',
        'INACTIVE'
    ) NOT NULL DEFAULT 'ACTIVE',

    PRIMARY KEY (id),
    UNIQUE KEY uk_brands_name (name)
) ENGINE=InnoDB;

-- =========================================================
-- 7. PRODUCTS
-- =========================================================
CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    update_date DATETIME(6),

    name VARCHAR(255) NOT NULL,
    description TEXT,

    category_id BIGINT NOT NULL,
    brand_id BIGINT NOT NULL,

    status ENUM(
        'ACTIVE',
        'INACTIVE'
    ) NOT NULL DEFAULT 'ACTIVE',

    PRIMARY KEY (id),

    KEY idx_products_category_id (category_id),
    KEY idx_products_brand_id (brand_id),
    KEY idx_products_status (status),

    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id),

    CONSTRAINT fk_products_brand
        FOREIGN KEY (brand_id)
        REFERENCES brands(id)
) ENGINE=InnoDB;

-- =========================================================
-- 8. PRODUCT VARIANTS
-- Thêm: unique (product_id, color, size, grip_size) để tránh
-- tạo trùng variant với SKU khác nhau.
-- Thêm CHECK cho price/stock.
-- =========================================================
CREATE TABLE product_variants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,

    sku VARCHAR(100) NOT NULL,

    color VARCHAR(100),
    size VARCHAR(50),
    weight INT,              -- gram, dùng để tính phí ship GHN
    length INT,               -- cm
    width INT,                 -- cm
    height INT,                -- cm
    grip_size VARCHAR(20),

    price DECIMAL(15,2) NOT NULL,
    sale_price DECIMAL(15,2),

    stock INT NOT NULL DEFAULT 0,

    status ENUM(
        'ACTIVE',
        'INACTIVE'
    ) NOT NULL DEFAULT 'ACTIVE',

    PRIMARY KEY (id),

    UNIQUE KEY uk_variants_sku (sku),
    UNIQUE KEY uk_variants_product_attrs (product_id, color, size, grip_size),
    KEY idx_variants_product_id (product_id),
    KEY idx_variants_status (status),

    CONSTRAINT fk_variants_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_variants_price CHECK (price >= 0),
    CONSTRAINT chk_variants_sale_price CHECK (sale_price IS NULL OR sale_price >= 0),
    CONSTRAINT chk_variants_stock CHECK (stock >= 0)
) ENGINE=InnoDB;

-- =========================================================
-- 9. PRODUCT IMAGES
-- FIX: thêm variant_id (nullable) để ảnh gắn theo từng màu.
-- NULL = ảnh chung ở cấp product, có giá trị = ảnh riêng theo variant/màu.
-- =========================================================
CREATE TABLE product_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    variant_id BIGINT,

    url VARCHAR(1024) NOT NULL,

    image_type ENUM(
        'MAIN',
        'NORMAL'
    ) NOT NULL DEFAULT 'NORMAL',

    order_index INT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    KEY idx_product_images_product_id (product_id),
    KEY idx_product_images_variant_id (variant_id),

    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_product_images_variant
        FOREIGN KEY (variant_id)
        REFERENCES product_variants(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 10. CART ITEMS
-- User cart only. Guest cart is stored in frontend localStorage.
-- Thêm CHECK quantity > 0.
-- =========================================================
CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,

    user_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,

    quantity INT NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_cart_user_variant (user_id, variant_id),
    KEY idx_cart_variant_id (variant_id),

    CONSTRAINT fk_cart_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_cart_variant
        FOREIGN KEY (variant_id)
        REFERENCES product_variants(id),

    CONSTRAINT chk_cart_quantity CHECK (quantity > 0)
) ENGINE=InnoDB;

-- =========================================================
-- 11. FLASH SALES
-- =========================================================
CREATE TABLE flash_sales (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    update_date DATETIME(6),

    name VARCHAR(255) NOT NULL,

    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,

    status ENUM(
        'SCHEDULED',
        'ACTIVE',
        'ENDED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'SCHEDULED',

    PRIMARY KEY (id),
    KEY idx_flash_sales_status (status),
    KEY idx_flash_sales_time (start_at, end_at)
) ENGINE=InnoDB;

-- =========================================================
-- 12. FLASH SALE ITEMS
-- =========================================================
CREATE TABLE flash_sale_items (
    id BIGINT NOT NULL AUTO_INCREMENT,

    flash_sale_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,

    sale_price DECIMAL(15,2) NOT NULL,
    sale_quantity INT NOT NULL,
    sold_quantity INT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_flash_sale_variant (
        flash_sale_id,
        variant_id
    ),

    KEY idx_flash_sale_items_variant (variant_id),

    CONSTRAINT fk_flash_sale_items_sale
        FOREIGN KEY (flash_sale_id)
        REFERENCES flash_sales(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_flash_sale_items_variant
        FOREIGN KEY (variant_id)
        REFERENCES product_variants(id),

    CONSTRAINT chk_flash_sale_price CHECK (sale_price >= 0),
    CONSTRAINT chk_flash_sale_qty CHECK (sale_quantity >= 0 AND sold_quantity >= 0)
) ENGINE=InnoDB;

-- =========================================================
-- 13. ORDERS
--
-- order_status:    trạng thái nghiệp vụ của shop
-- shipping_status:  trạng thái vận chuyển đã chuẩn hóa
-- shipping_carrier: đơn vị vận chuyển
--
-- Thêm các mốc thời gian (confirmed_at, cancelled_at,
-- delivered_at) để hỗ trợ tính SLA / báo cáo mà không phải
-- suy luận từ update_date chung chung.
--
-- SNAPSHOT (khác với addresses ở trên đang chuẩn hóa 3NF):
-- "address_id" chỉ mang tính THAM CHIẾU/tiện tra cứu (VD: gợi ý đặt lại
-- theo địa chỉ cũ) - KHÔNG phải nguồn sự thật cho thông tin giao hàng của
-- đơn hàng. Toàn bộ dữ liệu hiển thị/gửi GHN lấy từ "shipping_address_snapshot"
-- (đóng băng tại đúng thời điểm đặt hàng: tên người nhận, SĐT, địa chỉ chi
-- tiết, tỉnh/huyện/xã cả id lẫn tên).
--
-- Lý do bắt buộc phải snapshot: nếu chỉ trỏ FK và join sang addresses/
-- provinces/districts/wards để lấy dữ liệu hiển thị, thì khi khách sửa/xóa
-- địa chỉ trong sổ, hoặc khi Nhà nước sáp nhập/đổi tên địa giới 5 năm sau,
-- đơn hàng cũ sẽ hiển thị SAI so với lúc đặt hàng - vi phạm tính bất biến
-- của chứng từ lịch sử.
--
-- address_id dùng ON DELETE SET NULL (không CASCADE, không RESTRICT):
-- khách xóa địa chỉ gốc thì đơn hàng cũ mất liên kết tham chiếu (chấp nhận
-- được, vì không phải nguồn sự thật) nhưng KHÔNG bị xóa theo, và cũng
-- không bị chặn xóa địa chỉ chỉ vì có đơn hàng cũ trỏ tới.
-- =========================================================
CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    update_date DATETIME(6),

    user_id BIGINT NOT NULL,

    order_status ENUM(
        'PENDING_PAYMENT',
        'CONFIRMED',
        'PROCESSING',
        'SHIPPING',
        'DELIVERED',
        'CANCELLED',
        'COMPLETED'
    ) NOT NULL DEFAULT 'PENDING_PAYMENT',

    payment_method ENUM(
        'COD',
        'VNPAY',
        'MOMO'
    ) NOT NULL DEFAULT 'COD',

    shipping_status ENUM(
        'PENDING',
        'READY_TO_PICK',
        'PICKED_UP',
        'IN_TRANSIT',
        'OUT_FOR_DELIVERY',
        'DELIVERED',
        'DELIVERY_FAILED',
        'RETURNING',
        'RETURNED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',

    shipping_carrier ENUM(
        'GHN',
        'OTHER'
    ) NOT NULL DEFAULT 'GHN',

    tracking_number VARCHAR(100),

    -- Tham chiếu tới sổ địa chỉ - CHỈ để tra cứu, KHÔNG dùng để hiển thị/gửi
    -- GHN (xem shipping_address_snapshot bên dưới). Nullable vì có thể bị
    -- SET NULL khi địa chỉ gốc bị xóa.
    address_id BIGINT,

    -- Đóng băng toàn bộ thông tin giao hàng tại thời điểm đặt hàng, dạng JSON:
    -- {
    --   "recipientName": "Nguyễn Văn A",
    --   "phone": "0901234567",
    --   "detailAddress": "12 Ngõ 5 Láng Hạ",
    --   "provinceId": 201, "provinceName": "Hà Nội",
    --   "districtId": 1490, "districtName": "Quận Đống Đa",
    --   "wardCode": "1A0607", "wardName": "Phường Láng Hạ"
    -- }
    -- Đây mới là nguồn dữ liệu DUY NHẤT được dùng để hiển thị hóa đơn và
    -- gọi GHN API (map "detailAddress" -> "to_address" của GHN).
    shipping_address_snapshot JSON NOT NULL,

    subtotal DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    shipping_cost DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(15,2) NOT NULL DEFAULT 0.00,

    note VARCHAR(500),

    confirmed_at DATETIME(6),
    cancelled_at DATETIME(6),
    delivered_at DATETIME(6),

    -- GHN integration
    expected_delivery_time DATETIME(6),   -- GHN trả về khi tạo đơn
    ghn_service_id INT,                    -- service_id dùng khi gọi API GHN
    ghn_total_fee DECIMAL(15,2),           -- phí ship GHN trả về (có thể khác shipping_cost hiển thị cho khách)

    PRIMARY KEY (id),

    KEY idx_orders_user_id (user_id),
    KEY idx_orders_status (order_status),
    KEY idx_orders_shipping_status (shipping_status),
    KEY idx_orders_tracking_number (tracking_number),
    KEY idx_orders_address_id (address_id),

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    -- ON DELETE SET NULL: khách xóa địa chỉ gốc -> chỉ mất liên kết tham
    -- chiếu, đơn hàng cũ và shipping_address_snapshot của nó không bị ảnh
    -- hưởng (xem giải thích ở comment đầu bảng).
    CONSTRAINT fk_orders_address
        FOREIGN KEY (address_id)
        REFERENCES addresses(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_orders_amounts CHECK (
        subtotal >= 0 AND shipping_cost >= 0 AND
        discount >= 0 AND total >= 0
    )
) ENGINE=InnoDB;

-- =========================================================
-- 14. ORDER ITEMS
-- Snapshot sản phẩm tại thời điểm mua.
-- Thêm weight, grip_size để snapshot đầy đủ như product_variants.
-- FIX: variant_id ON DELETE SET NULL (không chặn xóa variant
-- cũ chỉ vì có order lịch sử tham chiếu tới).
-- =========================================================
CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,

    order_id BIGINT NOT NULL,
    variant_id BIGINT,

    sku VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,

    color VARCHAR(100),
    size VARCHAR(50),
    weight INT,
    grip_size VARCHAR(20),

    unit_price DECIMAL(15,2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(15,2) NOT NULL,

    PRIMARY KEY (id),

    KEY idx_order_items_order_id (order_id),
    KEY idx_order_items_variant_id (variant_id),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_items_variant
        FOREIGN KEY (variant_id)
        REFERENCES product_variants(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_order_items_qty CHECK (quantity > 0),
    CONSTRAINT chk_order_items_amounts CHECK (unit_price >= 0 AND line_total >= 0)
) ENGINE=InnoDB;

-- =========================================================
-- 15. PAYMENTS
--
-- Project nhỏ: PaymentTransaction được gộp vào payments.
--
-- FIX QUAN TRỌNG:
--  - Bỏ UNIQUE(order_id) -> cho phép nhiều payment attempt/order
--    (VNPay fail/timeout/hủy thì tạo lại attempt mới với txn_ref mới).
--  - Gộp payment_method + provider thành 1 cột duy nhất
--    (2 cột cùng enum là dư thừa với domain hiện tại).
--  - Thêm create_date/update_date để xác định attempt mới nhất
--    (dùng cùng order_id, ORDER BY create_date DESC LIMIT 1).
-- =========================================================
CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    update_date DATETIME(6),

    order_id BIGINT NOT NULL,

    provider ENUM(
        'COD',
        'VNPAY',
        'MOMO'
    ) NOT NULL,

    txn_ref VARCHAR(100),

    amount DECIMAL(15,2) NOT NULL,

    status ENUM(
        'PENDING',
        'PROCESSING',
        'PAID',
        'FAILED',
        'EXPIRED',
        'REFUNDED'
    ) NOT NULL DEFAULT 'PENDING',

    response_code VARCHAR(20),
    transaction_no VARCHAR(50),
    bank_code VARCHAR(30),

    pay_date DATETIME(6),
    expire_date DATETIME(6),

    failure_reason VARCHAR(500),

    PRIMARY KEY (id),

    KEY idx_payments_order_id (order_id),
    UNIQUE KEY uk_payments_txn_ref (txn_ref),
    KEY idx_payments_status (status),
    KEY idx_payments_order_status (order_id, status),

    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_payments_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

-- =========================================================
-- 16. SHIPPING TRACKING EVENTS
--
-- Lưu lịch sử để hiển thị timeline cho người dùng.
--
-- status:      trạng thái chuẩn hóa của hệ thống
-- ghn_status:  trạng thái gốc từ GHN
-- =========================================================
CREATE TABLE shipping_tracking_events (
    id BIGINT NOT NULL AUTO_INCREMENT,

    order_id BIGINT NOT NULL,

    status ENUM(
        'PENDING',
        'READY_TO_PICK',
        'PICKED_UP',
        'IN_TRANSIT',
        'OUT_FOR_DELIVERY',
        'DELIVERED',
        'DELIVERY_FAILED',
        'RETURNING',
        'RETURNED',
        'CANCELLED'
    ) NOT NULL,

    ghn_status ENUM(
        'READY_TO_PICK',
        'PICKING',
        'PICKED',
        'STORING',
        'TRANSPORTING',
        'SORTING',
        'DELIVERING',
        'DELIVERED',
        'DELIVERY_FAIL',
        'WAITING_TO_RETURN',
        'RETURN',
        'RETURN_TRANSPORTING',
        'RETURN_SORTING',
        'RETURNING',
        'RETURN_FAIL',
        'RETURNED',
        'EXCEPTION',
        'DAMAGE',
        'LOST',
        'CANCEL'
    ),

    title VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    reason_code VARCHAR(50),   -- GHN reason_code khi DELIVERY_FAIL / RETURN

    location VARCHAR(255),

    event_time DATETIME(6) NOT NULL,

    ghn_event_id VARCHAR(100),  -- id gốc từ webhook GHN nếu có, dùng chống trùng

    PRIMARY KEY (id),

    KEY idx_tracking_events_order (order_id),
    KEY idx_tracking_events_order_time (order_id, event_time),
    KEY idx_tracking_events_status (status),

    -- Chống insert trùng khi GHN gửi lại webhook (retry do timeout)
    UNIQUE KEY uk_tracking_ghn_event (order_id, ghn_status, event_time),

    CONSTRAINT fk_tracking_events_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;