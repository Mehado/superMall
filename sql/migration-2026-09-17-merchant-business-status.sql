-- ============================================================
-- 迁移脚本：商家自助营业状态落库（DB 为准，Redis 做缓存）
-- 适用：已经初始化过 supermall 库，需要在不重建数据的前提下升级
-- 执行：mysql -uroot -p supermall < sql/migration-2026-09-17-merchant-business-status.sql
-- 说明：全新安装请直接用 sql/supermall.sql，无需执行本脚本
-- ============================================================

USE supermall;

-- 商家自助营业状态：1营业中 0休息中（与平台状态 status 分离）
ALTER TABLE merchant
    ADD COLUMN business_status TINYINT NOT NULL DEFAULT 1 COMMENT '1营业中 0休息中（商家自助开关）' AFTER status;

-- 存量商家默认「营业中」。
-- 注意：升级前若某商家通过 Redis 设过「休息中」，该状态只在 Redis 里、无法迁移，
--       升级后需商家重新点一次「暂停营业」。
