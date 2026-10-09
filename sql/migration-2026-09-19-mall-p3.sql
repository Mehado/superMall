-- ============================================================
-- 迁移：电商 P3（购物车与地址）—— 2026-09-19
-- ------------------------------------------------------------
-- 已有库（已经跑过 sql/mall.sql 的）执行本脚本即可；
-- 全新安装直接跑 sql/mall.sql（已包含同样内容）。
--
-- 本脚本做两件事（幂等，可重复执行）：
--   1. 新建 mall_cart_item（购物车明细）
--   2. 给共享的 address 表补一个可空列 area_code（省市区末级编码）
--      —— 可空，对外卖业务零影响
--
-- 说明：
--   P3 不建 mall_area 表。省市区三级联动是静态区划数据，
--   直接放在 superMall-mall/src/main/resources/mall-area.json，
--   由 MallAreaService 启动时加载到内存（3800+ 条，一次加载、零查询）。
-- ============================================================

-- ---------- 1. 购物车明细 ----------
CREATE TABLE IF NOT EXISTS mall_cart_item (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL COMMENT 'sys_user.id（与外卖共享的账号）',
    product_id  BIGINT   NOT NULL,
    sku_id      BIGINT   NOT NULL COMMENT '加购粒度是 SKU；无规格商品也有默认 SKU',
    number      INT      NOT NULL DEFAULT 1,
    selected    TINYINT  NOT NULL DEFAULT 1 COMMENT '1 选中 0 未选（结算只看有效且选中的）',
    create_time DATETIME          DEFAULT NULL,
    update_time DATETIME          DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_cart_user_sku (user_id, sku_id),
    KEY idx_mall_cart_user (user_id, update_time)
) ENGINE = InnoDB COMMENT ='购物车明细（自营，无需商家维度）';

-- ---------- 2. 共享 address 表补列 ----------
-- MySQL 不支持 ADD COLUMN IF NOT EXISTS，用存储过程做幂等。
DROP PROCEDURE IF EXISTS mall_add_column_if_absent;
DELIMITER $$
CREATE PROCEDURE mall_add_column_if_absent(
    IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl VARCHAR(512))
BEGIN
    -- 只在表存在、且列不存在时才 ALTER；表不存在就跳过。
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

CALL mall_add_column_if_absent('address', 'area_code',
     'area_code VARCHAR(20) DEFAULT NULL COMMENT ''省市区末级编码（电商用，外卖可为空）'' AFTER district');
DROP PROCEDURE IF EXISTS mall_add_column_if_absent;

-- ---------- 核对 ----------
SELECT '购物车行' AS 项, COUNT(*) AS 值 FROM mall_cart_item
UNION ALL
SELECT 'address.area_code 列',
       COUNT(*) FROM information_schema.columns
       WHERE table_schema = DATABASE() AND table_name = 'address' AND column_name = 'area_code';
