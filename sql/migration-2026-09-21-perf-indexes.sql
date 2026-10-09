-- ============================================================
-- 迁移：性能索引补齐（2026-09-21）
-- ------------------------------------------------------------
-- 背景：安全/性能审计（node scripts/verify-security.mjs / docs/项目进度.md）
--      用 EXPLAIN 实测出「列表分页排序要 filesort」「统计走全表」，这里补 4 个索引：
--
--   1) orders(user_id, create_time)              外卖用户订单分页（order by create_time desc）
--   2) orders(merchant_id, create_time)          外卖商家订单分页
--   3) delivery_order(deliveryman_id, create_time) 骑手「我的配送」分页
--   4) mall_order(pay_status, pay_time)          电商看板/销售趋势（where pay_status='PAID' ... pay_time）
--
-- 说明：
--   · 1~3 是**改**已有索引（原来是单列 user_id / merchant_id / deliveryman_id，名字不变），
--     所以要先 DROP 再 ADD；4 是**新增**。
--   · MySQL 不支持 `ADD/DROP INDEX IF EXISTS`，沿用 mall.sql 里的存储过程套路做到**幂等**：
--     重复执行 = 删掉再建同一个索引，结果一致。
--   · 数据量小的时候优化器可能仍选全表扫描（那更便宜）；索引是给数据量上来后兜底的。
--
-- 执行：
--   mysql --ssl-mode=DISABLED --default-character-set=utf8mb4 -uroot -p supermall < sql/migration-2026-09-21-perf-indexes.sql
-- ============================================================

USE supermall;

-- ---------- 通用：先删（若存在）再建，保证列定义被更新且可重复执行 ----------
DROP PROCEDURE IF EXISTS supermall_replace_index;
DELIMITER $$
CREATE PROCEDURE supermall_replace_index(
    IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_cols VARCHAR(255))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE()
                 AND table_name = p_table AND index_name = p_index) THEN
        SET @drop_ddl = CONCAT('ALTER TABLE ', p_table, ' DROP INDEX ', p_index);
        PREPARE stmt FROM @drop_ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
    SET @add_ddl = CONCAT('ALTER TABLE ', p_table, ' ADD INDEX ', p_index, ' (', p_cols, ')');
    PREPARE stmt FROM @add_ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END$$
DELIMITER ;

CALL supermall_replace_index('orders',         'idx_orders_user',     'user_id, create_time');
CALL supermall_replace_index('orders',         'idx_orders_merchant', 'merchant_id, create_time');
CALL supermall_replace_index('delivery_order', 'idx_delivery_man',    'deliveryman_id, create_time');
CALL supermall_replace_index('mall_order',     'idx_mall_order_paid_time', 'pay_status, pay_time');

DROP PROCEDURE IF EXISTS supermall_replace_index;

-- ---------- 自检：打印这 4 个索引的最终形态 ----------
SELECT table_name AS 表, index_name AS 索引,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS 列
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND ((table_name = 'orders'          AND index_name IN ('idx_orders_user', 'idx_orders_merchant'))
    OR (table_name = 'delivery_order'  AND index_name = 'idx_delivery_man')
    OR (table_name = 'mall_order'      AND index_name = 'idx_mall_order_paid_time'))
GROUP BY table_name, index_name
ORDER BY table_name, index_name;
