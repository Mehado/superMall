-- ============================================================
-- superMall 在线外卖商城 - 数据库初始化脚本
-- 字符集：utf8mb4；引擎：InnoDB
-- 执行：mysql -uroot -p < sql/supermall.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS supermall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE supermall;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS delivery_order;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS address;
DROP TABLE IF EXISTS sku;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS deliveryman;
DROP TABLE IF EXISTS platform_admin;
DROP TABLE IF EXISTS merchant;
DROP TABLE IF EXISTS sys_user;

-- ------------------------------------------------------------
-- 用户账号
-- ------------------------------------------------------------
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    nickname    VARCHAR(50)           DEFAULT NULL,
    phone       VARCHAR(20)           DEFAULT NULL,
    avatar      VARCHAR(255)          DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB COMMENT ='用户账号';

-- ------------------------------------------------------------
-- 商家（一个商家一个账号，不设计内部员工与子角色）
-- ------------------------------------------------------------
CREATE TABLE merchant (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    shop_name   VARCHAR(100) NOT NULL,
    logo        VARCHAR(255)          DEFAULT NULL,
    description VARCHAR(500)          DEFAULT NULL,
    phone       VARCHAR(20)           DEFAULT NULL,
    address     VARCHAR(255)          DEFAULT NULL COMMENT '店铺地址（取餐点）',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1营业 0停业（平台状态）',
    business_status TINYINT      NOT NULL DEFAULT 1 COMMENT '1营业中 0休息中（商家自助开关）',
    -- 入驻审核（平台运营）：默认 1，保证历史/种子商家已通过，不受影响
    audit_status  TINYINT      NOT NULL DEFAULT 1 COMMENT '0待审核 1已通过 2已驳回',
    contact_name  VARCHAR(50)           DEFAULT NULL COMMENT '联系人',
    license_image VARCHAR(255)          DEFAULT NULL COMMENT '营业执照/门店照 URL',
    apply_time    DATETIME              DEFAULT NULL COMMENT '提交入驻申请时间',
    audit_time    DATETIME              DEFAULT NULL COMMENT '平台审核时间',
    reject_reason VARCHAR(255)          DEFAULT NULL COMMENT '驳回原因',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_username (username),
    KEY idx_merchant_audit_status (audit_status, apply_time)
) ENGINE = InnoDB COMMENT ='商家';

-- ------------------------------------------------------------
-- 平台运营（外卖）：审核商家入驻、管理骑手
-- 独立于商家 / 骑手 / 用户，请求头 platform-token
-- ------------------------------------------------------------
CREATE TABLE platform_admin (
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

-- ------------------------------------------------------------
-- 配送员（平台级，不固定属于某商家）
-- ------------------------------------------------------------
CREATE TABLE deliveryman (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    name        VARCHAR(50)           DEFAULT NULL,
    phone       VARCHAR(20)           DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1可接单 0停用',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_deliveryman_username (username)
) ENGINE = InnoDB COMMENT ='配送员';

-- ------------------------------------------------------------
-- 商品分类（merchant_id 实现多商家数据隔离）
-- ------------------------------------------------------------
CREATE TABLE category (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT      NOT NULL,
    business_type TINYINT   NOT NULL DEFAULT 1 COMMENT '1外卖食品 2百货商品',
    name        VARCHAR(50) NOT NULL,
    sort        INT         NOT NULL DEFAULT 0,
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME             DEFAULT NULL,
    update_time DATETIME             DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_category_merchant (merchant_id)
) ENGINE = InnoDB COMMENT ='商品分类';

-- ------------------------------------------------------------
-- 商品
-- ------------------------------------------------------------
CREATE TABLE product (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT         NOT NULL,
    category_id BIGINT         NOT NULL,
    business_type TINYINT      NOT NULL DEFAULT 1 COMMENT '1外卖食品 2百货商品',
    name        VARCHAR(100)   NOT NULL,
    image       VARCHAR(255)            DEFAULT NULL,
    description VARCHAR(500)            DEFAULT NULL,
    price       DECIMAL(10, 2) NOT NULL COMMENT '基础价格（无规格时即售价）',
    stock       INT            NOT NULL DEFAULT 0,
    sales       INT            NOT NULL DEFAULT 0 COMMENT '累计销量',
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    create_time DATETIME                DEFAULT NULL,
    update_time DATETIME                DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_product_merchant (merchant_id),
    KEY idx_product_category (category_id),
    KEY idx_product_business_status (business_type, status)
) ENGINE = InnoDB COMMENT ='商品';

-- ------------------------------------------------------------
-- 商品规格 SKU
-- ------------------------------------------------------------
CREATE TABLE sku (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    product_id  BIGINT         NOT NULL,
    merchant_id BIGINT         NOT NULL,
    name        VARCHAR(50)    NOT NULL COMMENT '规格名，如 大份/加辣',
    price       DECIMAL(10, 2) NOT NULL,
    stock       INT            NOT NULL DEFAULT 0,
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME                DEFAULT NULL,
    update_time DATETIME                DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_sku_product (product_id),
    KEY idx_sku_merchant (merchant_id)
) ENGINE = InnoDB COMMENT ='商品规格';

-- ------------------------------------------------------------
-- 收货地址
-- ------------------------------------------------------------
CREATE TABLE address (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    consignee   VARCHAR(50)  NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    province    VARCHAR(50)           DEFAULT NULL,
    city        VARCHAR(50)           DEFAULT NULL,
    district    VARCHAR(50)           DEFAULT NULL,
    detail      VARCHAR(255) NOT NULL,
    label       VARCHAR(20)           DEFAULT NULL COMMENT '家/公司/学校',
    is_default  TINYINT      NOT NULL DEFAULT 0 COMMENT '1默认地址 0非默认',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_address_user (user_id)
) ENGINE = InnoDB COMMENT ='收货地址';

-- ------------------------------------------------------------
-- 购物车（一个用户在每个商家下一辆购物车）
-- ------------------------------------------------------------
CREATE TABLE cart (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    merchant_id BIGINT   NOT NULL,
    create_time DATETIME          DEFAULT NULL,
    update_time DATETIME          DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user_merchant (user_id, merchant_id)
) ENGINE = InnoDB COMMENT ='购物车';

CREATE TABLE cart_item (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    cart_id      BIGINT         NOT NULL,
    user_id      BIGINT         NOT NULL,
    merchant_id  BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,
    sku_id       BIGINT         NOT NULL DEFAULT 0 COMMENT '0 表示无规格',
    product_name VARCHAR(100)            DEFAULT NULL,
    sku_name     VARCHAR(50)             DEFAULT NULL,
    image        VARCHAR(255)            DEFAULT NULL,
    price        DECIMAL(10, 2) NOT NULL COMMENT '加购时价格快照',
    number       INT            NOT NULL DEFAULT 1,
    create_time  DATETIME                DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_cart_item_cart (cart_id),
    KEY idx_cart_item_user (user_id)
) ENGINE = InnoDB COMMENT ='购物车明细';

-- ------------------------------------------------------------
-- 订单（merchant_id 数据隔离；submit_token 与 user_id 唯一索引做幂等兜底）
-- status: PENDING_PAYMENT / PAID / MERCHANT_ACCEPTED / WAITING_DELIVERY / DELIVERING / COMPLETED / CANCELLED
-- ------------------------------------------------------------
CREATE TABLE orders (
    id                      BIGINT         NOT NULL AUTO_INCREMENT,
    number                  VARCHAR(64)    NOT NULL COMMENT '订单号',
    submit_token            VARCHAR(64)             DEFAULT NULL COMMENT '下单幂等令牌',
    user_id                 BIGINT         NOT NULL,
    merchant_id             BIGINT         NOT NULL,
    address_id              BIGINT                  DEFAULT NULL,
    status                  VARCHAR(30)    NOT NULL DEFAULT 'PENDING_PAYMENT',
    pay_status              VARCHAR(20)    NOT NULL DEFAULT 'UN_PAID',
    pay_method              VARCHAR(20)             DEFAULT NULL COMMENT 'MOCK/WECHAT/ALIPAY',
    amount                  DECIMAL(10, 2) NOT NULL COMMENT '商品金额',
    delivery_fee            DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    remark                  VARCHAR(255)            DEFAULT NULL,
    consignee               VARCHAR(50)             DEFAULT NULL,
    phone                   VARCHAR(20)             DEFAULT NULL,
    address                 VARCHAR(255)            DEFAULT NULL,
    order_time              DATETIME                DEFAULT NULL,
    checkout_time           DATETIME                DEFAULT NULL,
    cancel_time             DATETIME                DEFAULT NULL,
    cancel_reason           VARCHAR(255)            DEFAULT NULL,
    delivery_time           DATETIME                DEFAULT NULL,
    estimated_delivery_time DATETIME                DEFAULT NULL,
    create_time             DATETIME                DEFAULT NULL,
    update_time             DATETIME                DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_number (number),
    UNIQUE KEY uk_orders_user_token (user_id, submit_token),
    -- 列表分页都是「按 user/merchant 过滤 + create_time 倒序」，
    -- 只带单列的索引会让排序退化成 filesort；带上 create_time 直接顺着索引取
    KEY idx_orders_user (user_id, create_time),
    KEY idx_orders_merchant (merchant_id, create_time),
    KEY idx_orders_status (status)
) ENGINE = InnoDB COMMENT ='订单';

CREATE TABLE order_item (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    order_id     BIGINT         NOT NULL,
    merchant_id  BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,
    sku_id       BIGINT         NOT NULL DEFAULT 0,
    product_name VARCHAR(100)            DEFAULT NULL,
    sku_name     VARCHAR(50)             DEFAULT NULL,
    image        VARCHAR(255)            DEFAULT NULL,
    price        DECIMAL(10, 2) NOT NULL,
    number       INT            NOT NULL,
    amount       DECIMAL(10, 2) NOT NULL COMMENT '小计',
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id)
) ENGINE = InnoDB COMMENT ='订单明细';

-- ------------------------------------------------------------
-- 配送单（与订单一对一，状态独立维护）
-- status: WAITING / ACCEPTED / DELIVERING / DELIVERED
-- ------------------------------------------------------------
CREATE TABLE delivery_order (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    order_id       BIGINT      NOT NULL,
    merchant_id    BIGINT      NOT NULL,
    deliveryman_id BIGINT               DEFAULT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    accept_time    DATETIME             DEFAULT NULL,
    pickup_time    DATETIME             DEFAULT NULL,
    delivered_time DATETIME             DEFAULT NULL,
    create_time    DATETIME             DEFAULT NULL,
    update_time    DATETIME             DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_delivery_order_order (order_id),
    KEY idx_delivery_order_status (status),
    KEY idx_delivery_man (deliveryman_id, create_time)
) ENGINE = InnoDB COMMENT ='配送单';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 初始化数据
-- 所有账号密码均为 Mall1234（8 位 + 大写 + 小写 + 数字，满足登录密码规则）
-- 账号一律使用邮箱：注册登录均校验「邮箱格式 + 常用邮箱后缀」
-- BCrypt: $2a$10$svb1PLFBGkwMQOMapKuT1uLSj8X.ZJpRpy8nyiUWpjiwH/plBbo.y
-- ============================================================

INSERT INTO sys_user (username, password, nickname, phone, status, create_time, update_time) VALUES
('user1@qq.com', '$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6', '小明', '13800000001', 1, NOW(), NOW()),
('user2@qq.com', '$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6', '小红', '13800000002', 1, NOW(), NOW());

INSERT INTO merchant (username, password, shop_name, logo, description, phone, address, status, create_time, update_time) VALUES
('merchant1@163.com', '$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6', '老王牛肉面', NULL, '现煮牛肉面，汤浓面劲道', '13900000001', '北京市海淀区中关村大街1号', 1, NOW(), NOW()),
('merchant2@163.com', '$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6', '轻食沙拉铺', NULL, '低卡轻食，健康每一天', '13900000002', '北京市海淀区上地信息路2号', 1, NOW(), NOW()),
('merchant3@126.com', '$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6', '惠民日用百货', NULL, '日用百货，送货上门', '13900000003', '北京市海淀区中关村南大街3号', 1, NOW(), NOW());

INSERT INTO deliveryman (username, password, name, phone, status, create_time, update_time) VALUES
('rider1@gmail.com', '$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6', '闪电侠', '13700000001', 1, NOW(), NOW()),
('rider2@gmail.com', '$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6', '风火轮', '13700000002', 1, NOW(), NOW());

-- 平台运营演示账号：platform@163.com / Admin1234（与商家/骑手/买家都不同）
INSERT INTO platform_admin (username, password, name, status, create_time, update_time) VALUES
('platform@163.com', '$2a$10$cUuwmfegKK7qRA5bpFNn2eeBi70aSR1XgjEJjQAkPljIxzv3xu7qa', '平台运营', 1, NOW(), NOW());

INSERT INTO category (merchant_id, business_type, name, sort, status, create_time, update_time) VALUES
(1, 1, '招牌面食', 1, 1, NOW(), NOW()),
(1, 1, '小食', 2, 1, NOW(), NOW()),
(1, 1, '饮品', 3, 1, NOW(), NOW()),
(2, 1, '沙拉', 1, 1, NOW(), NOW()),
(2, 1, '鲜榨果汁', 2, 1, NOW(), NOW()),
(3, 2, '日用清洁', 1, 1, NOW(), NOW()),
(3, 2, '纸巾湿巾', 2, 1, NOW(), NOW()),
(3, 2, '零食饮料', 3, 1, NOW(), NOW());

INSERT INTO product (merchant_id, category_id, business_type, name, image, description, price, stock, sales, status, create_time, update_time) VALUES
(1, 1, 1, '红烧牛肉面', 'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/beef-noodle.svg', '大块牛肉，秘制汤底', 28.00, 100, 0, 1, NOW(), NOW()),
(1, 1, 1, '番茄鸡蛋面', 'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/tomato-egg-noodle.svg', '酸甜番茄，营养均衡', 18.00, 100, 0, 1, NOW(), NOW()),
(1, 2, 1, '卤蛋', 'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/braised-egg.svg', '入味卤蛋', 3.00, 200, 0, 1, NOW(), NOW()),
(1, 3, 1, '冰镇酸梅汤', 'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/plum-drink.svg', '解腻消暑', 6.00, 200, 0, 1, NOW(), NOW()),
(2, 4, 1, '鸡胸肉沙拉', 'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/chicken-salad.svg', '优质蛋白，低脂低卡', 32.00, 80, 0, 1, NOW(), NOW()),
(2, 5, 1, '鲜榨橙汁', 'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/orange-juice.svg', '100% 鲜橙', 15.00, 100, 0, 1, NOW(), NOW()),
(1, 1, 1, '老北京炸酱面', 'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/zhajiang-noodle.svg', '黄豆酱炒肉丁，面码清爽', 26.00, 80, 0, 1, NOW(), NOW()),
(1, 2, 1, '凉拌黄瓜', 'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/cucumber-salad.svg', '拍黄瓜，蒜香开胃', 8.00, 120, 0, 1, NOW(), NOW()),
(1, 3, 1, '冰镇可乐', 'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/cola.svg', '冰到 4 度', 5.00, 200, 0, 1, NOW(), NOW()),
(1, 2, 1, '卤鸡翅', 'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/braised-egg.svg', '卤汁浸透，微辣', 12.00, 60, 0, 1, NOW(), NOW()),
(3, 6, 2, '洗衣液 2kg', NULL, '深层去渍，温和不伤手', 29.90, 50, 0, 1, NOW(), NOW()),
(3, 6, 2, '洗洁精 1.5kg', NULL, '柠檬清香，去油快', 15.90, 80, 0, 1, NOW(), NOW()),
(3, 7, 2, '抽纸 3层100抽*10包', NULL, '柔韧亲肤，家庭装', 19.90, 120, 0, 1, NOW(), NOW()),
(3, 8, 2, '矿泉水 550ml*12瓶', NULL, '天然弱碱性', 23.90, 100, 0, 1, NOW(), NOW());

-- 规格：红烧牛肉面提供 大份/小份 两种规格
INSERT INTO sku (product_id, merchant_id, name, price, stock, status, create_time, update_time) VALUES
(1, 1, '小份', 28.00, 60, 1, NOW(), NOW()),
(1, 1, '大份', 34.00, 40, 1, NOW(), NOW());

INSERT INTO address (user_id, consignee, phone, province, city, district, detail, label, is_default, create_time, update_time) VALUES
(1, '小明', '13800000001', '北京市', '北京市', '海淀区', '中关村软件园 8 号楼 801', '公司', 1, NOW(), NOW()),
(2, '小红', '13800000002', '北京市', '北京市', '朝阳区', '望京 SOHO T1 1203', '家', 1, NOW(), NOW());
