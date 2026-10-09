-- ============================================================
-- 迁移脚本（幂等版）：为商品引入「业态」维度（外卖食品 / 百货商品）
-- 适用：已经初始化过 supermall 库，需要在不重建数据的前提下升级
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 supermall < sql/migration-2026-09-17-business-type.sql
-- 说明：可重复执行；全新安装请直接用 sql/supermall.sql，无需执行本脚本
-- ============================================================

USE supermall;

-- 0) 字符集统一为 utf8mb4（解决 Incorrect string value 报错）
ALTER DATABASE supermall CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
ALTER TABLE merchant CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE category CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE product  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 1) 分类表：加业态（源头）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND COLUMN_NAME = 'business_type'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE category ADD COLUMN business_type TINYINT NOT NULL DEFAULT 1 COMMENT ''1外卖食品 2百货商品'' AFTER merchant_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 商品表：加业态（由分类派生）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'business_type'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE product ADD COLUMN business_type TINYINT NOT NULL DEFAULT 1 COMMENT ''1外卖食品 2百货商品'' AFTER category_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.1) 商品表：加联合索引（服务首页按业态随机）
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND INDEX_NAME = 'idx_product_business_status'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE product ADD KEY idx_product_business_status (business_type, status)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) 存量数据：按所属分类回填业态（分类默认 1，商品随之归入外卖食品）
UPDATE product p
    JOIN category c ON c.id = p.category_id
    SET p.business_type = c.business_type;

-- 4) 演示用的百货商家与商品（幂等：已存在则复用）
INSERT INTO merchant (username, password, shop_name, logo, description, phone, address, status, create_time, update_time)
SELECT 'merchant3', '$2a$10$svb1PLFBGkwMQOMapKuT1uLSj8X.ZJpRpy8nyiUWpjiwH/plBbo.y',
       '惠民日用百货', NULL, '日用百货，送货上门', '13900000003', '北京市海淀区中关村南大街3号', 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM merchant WHERE username = 'merchant3');

SET @m3 := (SELECT id FROM merchant WHERE username = 'merchant3' LIMIT 1);

-- 4.1) 演示分类（按 商家+名称 去重）
INSERT INTO category (merchant_id, business_type, name, sort, status, create_time, update_time)
SELECT @m3, 2, '日用清洁', 1, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM category WHERE merchant_id = @m3 AND name = '日用清洁');

INSERT INTO category (merchant_id, business_type, name, sort, status, create_time, update_time)
SELECT @m3, 2, '纸巾湿巾', 2, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM category WHERE merchant_id = @m3 AND name = '纸巾湿巾');

INSERT INTO category (merchant_id, business_type, name, sort, status, create_time, update_time)
SELECT @m3, 2, '零食饮料', 3, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM category WHERE merchant_id = @m3 AND name = '零食饮料');

SET @c_clean  := (SELECT id FROM category WHERE merchant_id = @m3 AND name = '日用清洁' LIMIT 1);
SET @c_tissue := (SELECT id FROM category WHERE merchant_id = @m3 AND name = '纸巾湿巾' LIMIT 1);
SET @c_drink  := (SELECT id FROM category WHERE merchant_id = @m3 AND name = '零食饮料' LIMIT 1);

-- 4.2) 演示商品（按 商家+名称 去重）
INSERT INTO product (merchant_id, category_id, business_type, name, image, description, price, stock, sales, status, create_time, update_time)
SELECT @m3, @c_clean, 2, '洗衣液 2kg', NULL, '深层去渍，温和不伤手', 29.90, 50, 0, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM product WHERE merchant_id = @m3 AND name = '洗衣液 2kg');

INSERT INTO product (merchant_id, category_id, business_type, name, image, description, price, stock, sales, status, create_time, update_time)
SELECT @m3, @c_clean, 2, '洗洁精 1.5kg', NULL, '柠檬清香，去油快', 15.90, 80, 0, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM product WHERE merchant_id = @m3 AND name = '洗洁精 1.5kg');

INSERT INTO product (merchant_id, category_id, business_type, name, image, description, price, stock, sales, status, create_time, update_time)
SELECT @m3, @c_tissue, 2, '抽纸 3层100抽*10包', NULL, '柔韧亲肤，家庭装', 19.90, 120, 0, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM product WHERE merchant_id = @m3 AND name = '抽纸 3层100抽*10包');

INSERT INTO product (merchant_id, category_id, business_type, name, image, description, price, stock, sales, status, create_time, update_time)
SELECT @m3, @c_drink, 2, '矿泉水 550ml*12瓶', NULL, '天然弱碱性', 23.90, 100, 0, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM product WHERE merchant_id = @m3 AND name = '矿泉水 550ml*12瓶');