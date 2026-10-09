-- ============================================================
-- 迁移：电商 P5（评价系统）—— 2026-09-21
-- ------------------------------------------------------------
-- 已有库（已经跑过 sql/mall.sql）执行本脚本即可；
-- 全新安装直接跑 sql/mall.sql（已包含同样内容）。
--
-- 本脚本做三件事（幂等，可重复执行）：
--   1. 给 mall_product 补 review_count / good_rate 两个派生列
--   2. 建 mall_review（评价主表，一个 order_item 只能评一次）
--   3. 建 mall_review_image（初评图 / 追评图）
-- ============================================================

DROP PROCEDURE IF EXISTS mall_add_column_if_absent;
DELIMITER $$
CREATE PROCEDURE mall_add_column_if_absent(
    IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl VARCHAR(512))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = p_table)
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = DATABASE()
                         AND table_name = p_table AND column_name = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_ddl);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL mall_add_column_if_absent('mall_product', 'review_count',
     'review_count INT NOT NULL DEFAULT 0 COMMENT ''评价数（P5 派生）''');
CALL mall_add_column_if_absent('mall_product', 'good_rate',
     'good_rate DECIMAL(5,2) NOT NULL DEFAULT 100.00 COMMENT ''好评率%（rating>=4，P5 派生）''');
DROP PROCEDURE IF EXISTS mall_add_column_if_absent;

CREATE TABLE IF NOT EXISTS mall_review (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    order_id       BIGINT        NOT NULL,
    order_number   VARCHAR(32)   NOT NULL,
    order_item_id  BIGINT        NOT NULL COMMENT '评价粒度 = 订单明细，一单一明细只能评一次',
    user_id        BIGINT        NOT NULL,
    product_id     BIGINT        NOT NULL,
    sku_id         BIGINT        NOT NULL,
    rating         TINYINT       NOT NULL COMMENT '1-5 星',
    content        VARCHAR(1000)          DEFAULT NULL,
    anonymous      TINYINT       NOT NULL DEFAULT 0 COMMENT '1 匿名 0 实名',
    follow_content VARCHAR(1000)          DEFAULT NULL COMMENT '追评内容',
    follow_time    DATETIME               DEFAULT NULL COMMENT '追评时间（非空即已追评）',
    create_time    DATETIME               DEFAULT NULL,
    update_time    DATETIME               DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_review_order_item (order_item_id),
    KEY idx_mall_review_product (product_id, create_time),
    KEY idx_mall_review_product_rating (product_id, rating),
    KEY idx_mall_review_user (user_id, create_time)
) ENGINE = InnoDB COMMENT ='商品评价';

CREATE TABLE IF NOT EXISTS mall_review_image (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    review_id   BIGINT       NOT NULL,
    url         VARCHAR(255) NOT NULL,
    type        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 初评 2 追评',
    sort        INT          NOT NULL DEFAULT 0,
    create_time DATETIME              DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_mall_review_image (review_id, type, sort)
) ENGINE = InnoDB COMMENT ='评价图片（晒图）';

-- ---------- 核对 ----------
SELECT '评价' AS 表, COUNT(*) AS 条数 FROM mall_review
UNION ALL SELECT '评价图', COUNT(*) FROM mall_review_image
UNION ALL SELECT '商品评价列', COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'mall_product'
      AND column_name IN ('review_count', 'good_rate');
