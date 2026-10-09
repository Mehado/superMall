-- ============================================================
-- superMall 外卖板块：商家入驻审核 + 平台运营（增量迁移）
-- 适用：已有库升级（幂等，可重复执行）
-- 全新安装请直接跑 sql/supermall.sql（已包含本文件全部内容）
--
-- 背景：外卖早就是「多商家 + 多骑手」架构（merchant_id 隔离、抢单池），
-- 但平台方没有任何入口可以新增商家/骑手。本脚本补上平台运营角色与其数据基础。
--
-- 设计要点：
--   1. audit_status 默认 1（已通过）—— 现有 3 个种子商家不受影响，零回归；
--      只有走 /api/merchant/apply 自助入驻的才是 audit_status=0 待审核。
--   2. 审核用条件更新 where audit_status = 0，并发/重复审核只会成功一次。
--   3. 平台运营账号独立成表（platform_admin），与商家/骑手/买家/电商管理员都是两套账号。
-- ============================================================

USE supermall;

-- ------------------------------------------------------------
-- merchant 加入驻审核相关列（幂等加列）
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS sm_add_column_if_absent;
DELIMITER $$
CREATE PROCEDURE sm_add_column_if_absent(
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

CALL sm_add_column_if_absent('merchant', 'audit_status',
     'audit_status TINYINT NOT NULL DEFAULT 1 COMMENT ''0待审核 1已通过 2已驳回''');
CALL sm_add_column_if_absent('merchant', 'contact_name',
     'contact_name VARCHAR(50) DEFAULT NULL COMMENT ''联系人''');
CALL sm_add_column_if_absent('merchant', 'license_image',
     'license_image VARCHAR(255) DEFAULT NULL COMMENT ''营业执照/门店照 URL''');
CALL sm_add_column_if_absent('merchant', 'apply_time',
     'apply_time DATETIME DEFAULT NULL COMMENT ''提交入驻申请时间''');
CALL sm_add_column_if_absent('merchant', 'audit_time',
     'audit_time DATETIME DEFAULT NULL COMMENT ''平台审核时间''');
CALL sm_add_column_if_absent('merchant', 'reject_reason',
     'reject_reason VARCHAR(255) DEFAULT NULL COMMENT ''驳回原因''');
DROP PROCEDURE IF EXISTS sm_add_column_if_absent;

-- 审核列表索引（幂等：MySQL 不支持 ADD INDEX IF NOT EXISTS，用存储过程探测）
DROP PROCEDURE IF EXISTS sm_add_index_if_absent;
DELIMITER $$
CREATE PROCEDURE sm_add_index_if_absent(
    IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_ddl VARCHAR(512))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = p_table)
       AND NOT EXISTS (SELECT 1 FROM information_schema.statistics
                       WHERE table_schema = DATABASE()
                         AND table_name = p_table AND index_name = p_index) THEN
        SET @ddl = p_ddl;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL sm_add_index_if_absent('merchant', 'idx_merchant_audit_status',
     'ALTER TABLE merchant ADD INDEX idx_merchant_audit_status (audit_status, apply_time)');
DROP PROCEDURE IF EXISTS sm_add_index_if_absent;

-- 历史商家统一视为「已通过」（默认值已保证，这里再兜一次 NULL）
UPDATE merchant SET audit_status = 1 WHERE audit_status IS NULL;

-- ------------------------------------------------------------
-- 平台运营账号表 + 演示账号
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS platform_admin (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL COMMENT '登录邮箱',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    name        VARCHAR(50)           DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_admin_username (username)
) ENGINE = InnoDB COMMENT ='平台运营（外卖）';

-- 演示账号：platform@163.com / Admin1234（密码哈希与电商管理员同源，均为 Admin1234）
INSERT INTO platform_admin (username, password, name, status, create_time, update_time)
VALUES ('platform@163.com',
        '$2a$10$cUuwmfegKK7qRA5bpFNn2eeBi70aSR1XgjEJjQAkPljIxzv3xu7qa',
        '平台运营', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;

-- ---------- 核对 ----------
SELECT '商家' AS 表, COUNT(*) AS 条数 FROM merchant
UNION ALL SELECT '待审核商家', COUNT(*) FROM merchant WHERE audit_status = 0
UNION ALL SELECT '平台运营', COUNT(*) FROM platform_admin
UNION ALL SELECT '骑手', COUNT(*) FROM deliveryman;
