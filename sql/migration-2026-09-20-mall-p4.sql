-- ============================================================
-- 迁移：电商 P4（订单履约）—— 2026-09-20
-- ------------------------------------------------------------
-- 已有库（已经跑过 sql/mall.sql）执行本脚本即可；
-- 全新安装直接跑 sql/mall.sql（已包含同样内容）。
--
-- 新建 4 张表（幂等，可重复执行）：
--   mall_order       订单主表（状态机 + 金额 + 收货快照）
--   mall_order_item  订单明细（下单快照）
--   mall_shipment    物流单（一单一张）
--   mall_refund      退货退款申请
-- ============================================================

CREATE TABLE IF NOT EXISTS mall_order (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    order_number        VARCHAR(32)   NOT NULL COMMENT '订单号：yyyyMMddHHmmssSSS + 3 位随机',
    user_id             BIGINT        NOT NULL,
    status              VARCHAR(32)   NOT NULL COMMENT 'PENDING_PAYMENT/PENDING_DELIVERY/PENDING_RECEIPT/COMPLETED/CANCELLED/REFUNDING/REFUNDED',
    pay_status          VARCHAR(16)   NOT NULL DEFAULT 'UN_PAID' COMMENT 'UN_PAID/PAID/REFUNDED',
    pay_method          VARCHAR(16)            DEFAULT NULL,
    total_amount        DECIMAL(12,2) NOT NULL COMMENT '商品总额',
    discount_amount     DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额（P6 优惠券）',
    freight_amount      DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    pay_amount          DECIMAL(12,2) NOT NULL COMMENT '实付 = 商品 - 优惠 + 运费',
    receiver_name       VARCHAR(50)   NOT NULL,
    receiver_phone      VARCHAR(20)   NOT NULL,
    receiver_province   VARCHAR(50)            DEFAULT NULL,
    receiver_city       VARCHAR(50)            DEFAULT NULL,
    receiver_district   VARCHAR(50)            DEFAULT NULL,
    receiver_area_code  VARCHAR(20)            DEFAULT NULL,
    receiver_detail     VARCHAR(255)  NOT NULL,
    remark              VARCHAR(255)           DEFAULT NULL,
    submit_token        VARCHAR(64)            DEFAULT NULL COMMENT '下单幂等令牌',
    cancel_reason       VARCHAR(255)           DEFAULT NULL,
    cancel_time         DATETIME               DEFAULT NULL,
    pay_time            DATETIME               DEFAULT NULL,
    delivery_time       DATETIME               DEFAULT NULL COMMENT '发货时间',
    receive_time        DATETIME               DEFAULT NULL COMMENT '收货时间',
    expire_time         DATETIME               DEFAULT NULL COMMENT '支付截止，超时自动取消',
    create_time         DATETIME               DEFAULT NULL,
    update_time         DATETIME               DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_order_number (order_number),
    UNIQUE KEY uk_mall_order_user_token (user_id, submit_token),
    KEY idx_mall_order_user (user_id, create_time),
    KEY idx_mall_order_status_expire (status, expire_time)
) ENGINE = InnoDB COMMENT ='订单';

CREATE TABLE IF NOT EXISTS mall_order_item (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    order_number VARCHAR(32)   NOT NULL,
    product_id   BIGINT        NOT NULL,
    sku_id       BIGINT        NOT NULL,
    product_name VARCHAR(100)  NOT NULL,
    sku_code     VARCHAR(64)            DEFAULT NULL,
    image        VARCHAR(255)           DEFAULT NULL,
    spec_text    VARCHAR(200)           DEFAULT NULL COMMENT '规格文案，如「白 / M」',
    price        DECIMAL(10,2) NOT NULL,
    number       INT           NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_mall_order_item_order (order_id)
) ENGINE = InnoDB COMMENT ='订单明细（下单快照）';

CREATE TABLE IF NOT EXISTS mall_shipment (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    order_id     BIGINT      NOT NULL,
    order_number VARCHAR(32) NOT NULL,
    company      VARCHAR(50) NOT NULL COMMENT '物流公司',
    tracking_no  VARCHAR(64) NOT NULL COMMENT '物流单号',
    ship_time    DATETIME             DEFAULT NULL,
    create_time  DATETIME             DEFAULT NULL,
    update_time  DATETIME             DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_shipment_order (order_id)
) ENGINE = InnoDB COMMENT ='物流单';

CREATE TABLE IF NOT EXISTS mall_refund (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    refund_number   VARCHAR(32)   NOT NULL,
    order_id        BIGINT        NOT NULL,
    order_number    VARCHAR(32)   NOT NULL,
    user_id         BIGINT        NOT NULL,
    type            TINYINT       NOT NULL DEFAULT 1 COMMENT '1 仅退款 2 退货退款',
    reason          VARCHAR(255)  NOT NULL,
    description     VARCHAR(500)           DEFAULT NULL,
    amount          DECIMAL(12,2) NOT NULL COMMENT '退款金额（= 订单实付）',
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    previous_status VARCHAR(32)            DEFAULT NULL COMMENT '申请前的订单状态，驳回时回退用',
    admin_remark    VARCHAR(255)           DEFAULT NULL,
    audit_time      DATETIME               DEFAULT NULL,
    create_time     DATETIME               DEFAULT NULL,
    update_time     DATETIME               DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_refund_number (refund_number),
    KEY idx_mall_refund_order (order_id),
    KEY idx_mall_refund_status (status, create_time)
) ENGINE = InnoDB COMMENT ='退货退款申请';

-- ---------- 核对 ----------
SELECT '订单' AS 表, COUNT(*) AS 条数 FROM mall_order
UNION ALL SELECT '订单明细', COUNT(*) FROM mall_order_item
UNION ALL SELECT '物流单', COUNT(*) FROM mall_shipment
UNION ALL SELECT '退款申请', COUNT(*) FROM mall_refund;
