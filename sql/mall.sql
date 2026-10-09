-- ============================================================
-- superMall 电商板块（mall）建表脚本
-- ------------------------------------------------------------
-- 【与外卖板块的隔离约定 —— 三条红线】
--   1. 表名一律 mall_ 前缀，绝不出现外卖那 12 张表的名字
--      （address / cart / cart_item / category / delivery_order / deliveryman /
--        merchant / order_item / orders / product / sku / sys_user）
--   2. Redis key 一律 mall: 前缀，绝不与外卖的 stock:* / order:* / merchant:* 重名
--      —— 重名会导致库存串号
--   3. MQ exchange/queue 一律 mall. 前缀，绝不与外卖的 order.* / pay.* 重名
--      —— 重名会让两个服务抢同一条消息
--
-- 【共享的两张表（只读/约定式写入，不改语义）】
--   sys_user —— 商城用户就是外卖用户，一个账号两边通用
--   address  —— 收货地址共用（后续阶段会加一个可空列 area_code，不影响外卖）
--
-- 执行方式（本机）：
--   mysql --ssl-mode=DISABLED --default-character-set=utf8mb4 -uroot -p supermall < sql/mall.sql
--
-- 幂等：全部使用 CREATE TABLE IF NOT EXISTS / INSERT ... ON DUPLICATE KEY UPDATE
-- ============================================================

-- 与外卖【同一个库 supermall】：这里显式选中，保证
--   ① 单独执行（mysql < sql/mall.sql）不用额外指定库
--   ② Docker 初始化脚本（docker-entrypoint-initdb.d）顺序执行时也能落到正确的库
CREATE DATABASE IF NOT EXISTS supermall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE supermall;

-- ---------- P0：运营后台管理员 ----------
-- 为什么单独一张表而不是复用 sys_user：
--   管理员是平台内部角色，与商城买家职责完全不同；
--   复用 sys_user 就得给它加 role 语义 → 那就动了外卖的表结构，违反“不重构”。
CREATE TABLE IF NOT EXISTS mall_admin (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL COMMENT '登录邮箱，唯一',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    name        VARCHAR(50)           DEFAULT NULL COMMENT '显示名',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_admin_username (username)
) ENGINE = InnoDB COMMENT ='商城运营后台管理员';

-- 演示管理员：admin@qq.com / Admin1234
-- （与买家演示密码 Mall1234 刻意不同：管理员权限更高）
INSERT INTO mall_admin (username, password, name, status, create_time, update_time)
VALUES ('admin@qq.com',
        '$2a$10$cUuwmfegKK7qRA5bpFNn2eeBi70aSR1XgjEJjQAkPljIxzv3xu7qa',
        '平台管理员', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), name = VALUES(name), status = 1;

-- ============================================================
-- P1：商品体系
-- ------------------------------------------------------------
-- 设计要点：
--   1. 【不做“无规格”特例】：无规格商品 = 1 个不关联任何规格值的默认 SKU。
--      下单/库存/购物车只认 SKU 一套逻辑，不用写分支。
--      （外卖那边 product.stock 与 sku.stock 双轨，导致“无规格要传 skuId=0”这个坑）
--   2. SPU 上的 price_low / price_high / stock 是【由 SKU 派生的冗余字段】，
--      列表页只读 SPU 就能展示价格区间与库存，不用 JOIN。
--   3. 分类最多 3 级（parent_id + level）；商品挂在【叶子分类】上。
--   4. SKU 数量必须 = 各规格值数量的乘积，服务端强制校验（见 MallProductServiceImpl）。
-- ============================================================

-- ---------- 品牌 ----------
CREATE TABLE IF NOT EXISTS mall_brand (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL COMMENT '品牌名',
    logo        VARCHAR(255)          DEFAULT NULL,
    description VARCHAR(500)          DEFAULT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_brand_name (name)
) ENGINE = InnoDB COMMENT ='商品品牌';

-- ---------- 多级分类 ----------
CREATE TABLE IF NOT EXISTS mall_category (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '0=顶级',
    level       TINYINT     NOT NULL DEFAULT 1 COMMENT '1/2/3，最多 3 级',
    name        VARCHAR(50) NOT NULL,
    icon        VARCHAR(255)         DEFAULT NULL,
    sort        INT         NOT NULL DEFAULT 0,
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME             DEFAULT NULL,
    update_time DATETIME             DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_category_parent_name (parent_id, name),
    KEY idx_mall_category_parent (parent_id)
) ENGINE = InnoDB COMMENT ='商品分类（多级）';

-- ---------- 商品 SPU ----------
CREATE TABLE IF NOT EXISTS mall_product (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    category_id BIGINT         NOT NULL COMMENT '叶子分类 id',
    brand_id    BIGINT                  DEFAULT NULL,
    name        VARCHAR(100)   NOT NULL,
    subtitle    VARCHAR(200)            DEFAULT NULL COMMENT '卖点副标题',
    main_image  VARCHAR(255)            DEFAULT NULL COMMENT '主图（与 mall_product_image 的第一张一致）',
    detail      TEXT                    DEFAULT NULL COMMENT '详情（长文本）',
    price_low   DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '派生：MIN(sku.price)',
    price_high  DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '派生：MAX(sku.price)',
    stock       INT            NOT NULL DEFAULT 0 COMMENT '派生：SUM(sku.stock)',
    sales       INT            NOT NULL DEFAULT 0,
    rating      DECIMAL(3, 2)  NOT NULL DEFAULT 5.00 COMMENT 'P5 评价系统维护',
    status      TINYINT        NOT NULL DEFAULT 0 COMMENT '1上架 0下架（新建默认下架，SKU 配好再上架）',
    sort        INT            NOT NULL DEFAULT 0,
    create_time DATETIME                DEFAULT NULL,
    update_time DATETIME                DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_mall_product_category (category_id),
    KEY idx_mall_product_brand (brand_id),
    KEY idx_mall_product_status_sort (status, sort, id)
) ENGINE = InnoDB COMMENT ='商品 SPU';

-- ---------- 商品多图 ----------
CREATE TABLE IF NOT EXISTS mall_product_image (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    product_id  BIGINT       NOT NULL,
    url         VARCHAR(255) NOT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    create_time DATETIME              DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_mall_product_image_product (product_id)
) ENGINE = InnoDB COMMENT ='商品图片（多图）';

-- ---------- 规格项（颜色 / 尺码）----------
CREATE TABLE IF NOT EXISTS mall_spec (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    product_id  BIGINT      NOT NULL,
    name        VARCHAR(30) NOT NULL COMMENT '规格名，如 颜色/尺码',
    sort        INT         NOT NULL DEFAULT 0,
    create_time DATETIME             DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_spec_product_name (product_id, name)
) ENGINE = InnoDB COMMENT ='商品规格项';

-- ---------- 规格值（白/黑 / M/L）----------
CREATE TABLE IF NOT EXISTS mall_spec_value (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    spec_id     BIGINT      NOT NULL,
    value       VARCHAR(50) NOT NULL COMMENT '规格值，如 白/黑/M/L',
    sort        INT         NOT NULL DEFAULT 0,
    create_time DATETIME             DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_spec_value (spec_id, value),
    KEY idx_mall_spec_value_spec (spec_id)
) ENGINE = InnoDB COMMENT ='规格值';

-- ---------- SKU（规格组合）----------
CREATE TABLE IF NOT EXISTS mall_sku (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    product_id  BIGINT         NOT NULL,
    sku_code    VARCHAR(64)             DEFAULT NULL COMMENT '商家编码（可空）',
    price       DECIMAL(10, 2) NOT NULL,
    stock       INT            NOT NULL DEFAULT 0,
    sales       INT            NOT NULL DEFAULT 0,
    image       VARCHAR(255)            DEFAULT NULL,
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME                DEFAULT NULL,
    update_time DATETIME                DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_sku_code (product_id, sku_code),
    KEY idx_mall_sku_product (product_id)
) ENGINE = InnoDB COMMENT ='商品 SKU';

-- ---------- SKU ↔ 规格值 关联 ----------
-- 带上 spec_id 而不是只存 spec_value_id：渲染规格矩阵时要按规格项分组。
CREATE TABLE IF NOT EXISTS mall_sku_spec (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    sku_id        BIGINT NOT NULL,
    spec_id       BIGINT NOT NULL,
    spec_value_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_sku_spec (sku_id, spec_id),
    KEY idx_mall_sku_spec_value (spec_value_id)
) ENGINE = InnoDB COMMENT ='SKU 与规格值的关联';

-- ============================================================
-- P1 种子数据（幂等：全部 ON DUPLICATE KEY UPDATE）
-- 图片统一留空（main_image = NULL）—— P1 交付的是【多图上传能力】，
-- 演示图待补；前端需做占位处理。
-- ============================================================

-- 品牌
INSERT INTO mall_brand (id, name, description, sort, status, create_time, update_time) VALUES
(1, '棉致',   '舒适基础款',     1, 1, NOW(), NOW()),
(2, '峰行',   '户外与运动',     2, 1, NOW(), NOW()),
(3, '知味',   '日常食饮',       3, 1, NOW(), NOW()),
(4, '简木',   '家居生活',       4, 1, NOW(), NOW()),
(5, '光年',   '数码配件',       5, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

-- 多级分类（3 级）
INSERT INTO mall_category (id, parent_id, level, name, sort, status, create_time, update_time) VALUES
(1,  0, 1, '服饰鞋包', 1, 1, NOW(), NOW()),
(2,  1, 2, '男装',    1, 1, NOW(), NOW()),
(3,  1, 2, '鞋靴',    2, 1, NOW(), NOW()),
(4,  2, 3, 'T恤',     1, 1, NOW(), NOW()),
(5,  2, 3, '衬衫',    2, 1, NOW(), NOW()),
(6,  3, 3, '运动鞋',  1, 1, NOW(), NOW()),
(7,  0, 1, '食品饮料', 2, 1, NOW(), NOW()),
(8,  7, 2, '冲饮',    1, 1, NOW(), NOW()),
(9,  8, 3, '咖啡',    1, 1, NOW(), NOW()),
(10, 0, 1, '数码配件', 3, 1, NOW(), NOW()),
(11, 10, 2, '充电',   1, 1, NOW(), NOW()),
(12, 11, 3, '充电宝', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), level = VALUES(level), sort = VALUES(sort);

-- 商品 1：T恤（颜色 2 × 尺码 2 = 4 个 SKU）
INSERT INTO mall_product (id, category_id, brand_id, name, subtitle, detail, status, sort, create_time, update_time)
VALUES (1, 4, 1, '纯棉圆领T恤', '260g 重磅纯棉，不易变形', '100% 精梳棉，亲肤透气。', 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), category_id = VALUES(category_id);

-- 商品 2：运动鞋（颜色 2 × 尺码 2 = 4 个 SKU）
INSERT INTO mall_product (id, category_id, brand_id, name, subtitle, detail, status, sort, create_time, update_time)
VALUES (2, 6, 2, '轻便运动鞋', '回弹中底，久站不累', '飞织鞋面，重量约 230g。', 0, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), category_id = VALUES(category_id);

-- 商品 3：咖啡液（无规格 → 1 个默认 SKU）
INSERT INTO mall_product (id, category_id, brand_id, name, subtitle, detail, status, sort, create_time, update_time)
VALUES (3, 9, 3, '冷萃咖啡液', '0 糖 0 脂，加奶即饮', '常温保存，拧开即用。', 0, 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), category_id = VALUES(category_id);

-- 规格项：商品1 颜色/尺码；商品2 颜色/尺码
INSERT INTO mall_spec (id, product_id, name, sort, create_time) VALUES
(1, 1, '颜色', 1, NOW()), (2, 1, '尺码', 2, NOW()),
(3, 2, '颜色', 1, NOW()), (4, 2, '尺码', 2, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 规格值
INSERT INTO mall_spec_value (id, spec_id, value, sort, create_time) VALUES
(1, 1, '白', 1, NOW()), (2, 1, '黑', 2, NOW()),
(3, 2, 'M',  1, NOW()), (4, 2, 'L',  2, NOW()),
(5, 3, '灰', 1, NOW()), (6, 3, '黑', 2, NOW()),
(7, 4, '40', 1, NOW()), (8, 4, '41', 2, NOW())
ON DUPLICATE KEY UPDATE value = VALUES(value);

-- SKU
INSERT INTO mall_sku (id, product_id, sku_code, price, stock, status, create_time, update_time) VALUES
(1, 1, 'TS-W-M',  79.00, 100, 1, NOW(), NOW()),
(2, 1, 'TS-W-L',  79.00, 120, 1, NOW(), NOW()),
(3, 1, 'TS-B-M',  82.00,  90, 1, NOW(), NOW()),
(4, 1, 'TS-B-L',  82.00,  80, 1, NOW(), NOW()),
(5, 2, 'SH-G-40', 299.00, 50, 1, NOW(), NOW()),
(6, 2, 'SH-G-41', 299.00, 40, 1, NOW(), NOW()),
(7, 2, 'SH-B-40', 309.00, 30, 1, NOW(), NOW()),
(8, 2, 'SH-B-41', 309.00, 25, 1, NOW(), NOW()),
(9, 3, NULL,       39.90, 500, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE price = VALUES(price), stock = VALUES(stock);

-- SKU ↔ 规格值
INSERT INTO mall_sku_spec (id, sku_id, spec_id, spec_value_id) VALUES
(1, 1, 1, 1), (2, 1, 2, 3),
(3, 2, 1, 1), (4, 2, 2, 4),
(5, 3, 1, 2), (6, 3, 2, 3),
(7, 4, 1, 2), (8, 4, 2, 4),
(9, 5, 3, 5), (10, 5, 4, 7),
(11, 6, 3, 5), (12, 6, 4, 8),
(13, 7, 3, 6), (14, 7, 4, 7),
(15, 8, 3, 6), (16, 8, 4, 8)
ON DUPLICATE KEY UPDATE spec_value_id = VALUES(spec_value_id);

-- 把 SPU 上的冗余价格/库存回填一次（与上面 SKU 保持一致）
UPDATE mall_product p SET
    p.price_low  = (SELECT MIN(s.price) FROM mall_sku s WHERE s.product_id = p.id AND s.status = 1),
    p.price_high = (SELECT MAX(s.price) FROM mall_sku s WHERE s.product_id = p.id AND s.status = 1),
    p.stock      = (SELECT COALESCE(SUM(s.stock), 0) FROM mall_sku s WHERE s.product_id = p.id AND s.status = 1)
WHERE EXISTS (SELECT 1 FROM mall_sku s WHERE s.product_id = p.id);

-- ============================================================
-- P2：浏览与发现
-- ------------------------------------------------------------
-- 设计要点：
--   1. 搜索先用 MySQL LIKE（商品名/副标题/品牌名/分类名），
--      ES + 高亮 + 拼音是 P8 进阶项，届时换掉这里即可（对外接口形态不变）。
--   2. 排序只允许白名单枚举（orderBy），在 XML 里用 <choose> 展开，
--      不做字符串拼 SQL —— 避免注入。
--   3. 价格区间筛选用「区间有交集」语义：price_high >= min AND price_low <= max。
--      比「最低价落在区间内」更符合直觉（该商品有 SKU 落在这个价位）。
--   4. 浏览足迹用 (user_id, product_id) 唯一键 + ON DUPLICATE KEY UPDATE 去重，
--      重复浏览只更新时间与次数，不会把足迹刷成一堆重复行。
-- ============================================================

-- ---------- 排序/筛选用的索引（都是「上架 + 排序字段」的组合） ----------
-- product 上已有 idx_mall_product_status_sort；下面按需要排序的字段各加一个。
-- 幂等：MySQL 不支持 ADD INDEX IF NOT EXISTS，所以先建表后再用存储过程判断。
DROP PROCEDURE IF EXISTS mall_add_index_if_absent;
DELIMITER $$
CREATE PROCEDURE mall_add_index_if_absent(
    IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_cols VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = p_table AND index_name = p_index) THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_table, ' ADD INDEX ', p_index, ' (', p_cols, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL mall_add_index_if_absent('mall_product', 'idx_mall_product_status_sales',  'status, sales');
CALL mall_add_index_if_absent('mall_product', 'idx_mall_product_status_rating', 'status, rating');
CALL mall_add_index_if_absent('mall_product', 'idx_mall_product_status_price',  'status, price_low');
CALL mall_add_index_if_absent('mall_product', 'idx_mall_product_status_new',    'status, create_time');
DROP PROCEDURE IF EXISTS mall_add_index_if_absent;

-- ---------- 推荐位 / 轮播图 ----------
CREATE TABLE IF NOT EXISTS mall_banner (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL COMMENT '标题（后台识别用）',
    image       VARCHAR(255) NOT NULL COMMENT '图片 URL',
    position    TINYINT      NOT NULL DEFAULT 1 COMMENT '1首页轮播 2首页金刚区 3分类页顶部',
    link_type   TINYINT      NOT NULL DEFAULT 1 COMMENT '1商品 2分类 3外部链接 0无跳转',
    link_value  VARCHAR(255)          DEFAULT NULL COMMENT '商品id / 分类id / 外链URL',
    sort        INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1上线 0下线',
    start_time  DATETIME              DEFAULT NULL COMMENT '生效开始，NULL=不限',
    end_time    DATETIME              DEFAULT NULL COMMENT '生效结束，NULL=不限',
    create_time DATETIME              DEFAULT NULL,
    update_time DATETIME              DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_mall_banner_pos (position, status, sort, id)
) ENGINE = InnoDB COMMENT ='首页推荐位 / 轮播图';

-- ---------- 收藏夹 ----------
-- 唯一键保证同一用户对同一商品只有一条收藏，重复收藏不会产生重复行。
CREATE TABLE IF NOT EXISTS mall_favorite (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL COMMENT 'sys_user.id（与外卖共享的账号）',
    product_id  BIGINT   NOT NULL,
    create_time DATETIME          DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_favorite (user_id, product_id),
    KEY idx_mall_favorite_user (user_id, create_time)
) ENGINE = InnoDB COMMENT ='商品收藏夹';

-- ---------- 浏览足迹 ----------
CREATE TABLE IF NOT EXISTS mall_footprint (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    user_id      BIGINT   NOT NULL COMMENT 'sys_user.id',
    product_id   BIGINT   NOT NULL,
    visit_count  INT      NOT NULL DEFAULT 1 COMMENT '浏览次数',
    create_time  DATETIME          DEFAULT NULL COMMENT '首次浏览',
    update_time  DATETIME          DEFAULT NULL COMMENT '最近浏览（列表按它倒序）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_footprint (user_id, product_id),
    KEY idx_mall_footprint_user (user_id, update_time)
) ENGINE = InnoDB COMMENT ='商品浏览足迹';

-- ---------- P2 种子：几个轮播位 ----------
INSERT INTO mall_banner (id, title, image, position, link_type, link_value, sort, status, create_time, update_time) VALUES
(1, '首页banner-新款T恤', 'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/placeholder-banner-1.svg', 1, 1, '1', 1, 0, NOW(), NOW()),
(2, '首页banner-运动鞋',   'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/placeholder-banner-2.svg', 1, 1, '2', 2, 0, NOW(), NOW()),
(3, '首页金刚区-咖啡',     'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/placeholder-banner-3.svg', 2, 2, '9', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), image = VALUES(image), position = VALUES(position);

-- 注意：种子 banner 的 status 默认 0（下线），因为上面的图片 URL 是占位地址、并不存在。
-- 运营上传真实图片后，在后台把 status 改成 1 即可上线，避免前台出现碎图。

-- ============================================================
-- P3：购物车与地址
-- ------------------------------------------------------------
-- 设计要点：
--   1. 【不存价格快照】。购物车只存 product_id + sku_id + number，
--      每次读购物车 JOIN mall_sku 取「当前价」，满足「价格实时计算」。
--      （外卖的 cart_item 存 price 快照，那是另一种取舍；电商这边是自营、
--       价格由平台控制，实时价更符合预期。）
--   2. 【自营不需要购物车头表】。外卖按 `用户 × 商家` 一辆购物车，
--      电商不引入商家概念，所以直接一张 mall_cart_item，
--      用 (user_id, sku_id) 唯一键保证「同一 SKU 只有一行」。
--   3. 【同商品不同 SKU 是两行】。加购粒度是 SKU 而不是 SPU，
--      否则 79 元的 M 码和 82 元的 L 码没法在一个购物车里共存。
--   4. 【失效商品不物理删除】。商品下架 / SKU 被删 / 库存不足，
--      列表里照常返回并给出 invalidReason，由买家自己决定清理。
--      否则用户会以为东西「凭空消失」。
--   5. 省市区三级联动用静态区划数据（resources/mall-area.json），
--      只在 address 上加一个可空列 area_code 存末级编码，
--      同时冗余 province/city/district 名称，外卖侧读这张表零影响。
-- ============================================================

-- ---------- 购物车明细 ----------
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

-- ---------- 共享 address 表：补一个可空列 area_code ----------
-- MySQL 不支持 ADD COLUMN IF NOT EXISTS，用存储过程做幂等（与上面的加索引同理）。
-- 列可空：外卖侧写入的地址不填它，电商侧新地址会填。
DROP PROCEDURE IF EXISTS mall_add_column_if_absent;
DELIMITER $$
CREATE PROCEDURE mall_add_column_if_absent(
    IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl VARCHAR(512))
BEGIN
    -- 只在表存在、且列不存在时才 ALTER；表不存在（尚未跑 supermall.sql）就跳过，
    -- 保证 mall.sql 单独执行也不会报错。
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

-- ============================================================
-- P4：订单履约
-- ------------------------------------------------------------
-- 设计要点：
--   1. 【状态全部条件更新】 待付款→待发货→待收货→已完成，异常分支取消/退款中/已退款；
--      所有流转用 where status = 期望值，天然幂等 + 防并发重复流转。
--   2. 【金额服务端算】 total - discount + freight = pay，前端传不了价格；
--   3. 【明细是快照】 商品改名/改价/下架不影响历史订单；
--   4. 【收货信息是快照】 地址改删不影响历史订单。
--   5. 【防超卖两层】 Redis 预扣（Lua）+ DB 条件扣减（stock >= number）。
--   6. 【超时取消】 定时任务每分钟扫 expire_time < now 的待付款单，条件取消后回补库存。
-- ============================================================

-- ---------- 订单主表 ----------
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
    KEY idx_mall_order_status_expire (status, expire_time),
    -- P7 看板/趋势：where pay_status='PAID' and pay_time >= ?  group by date(pay_time)
    KEY idx_mall_order_paid_time (pay_status, pay_time)
) ENGINE = InnoDB COMMENT ='订单';

-- ---------- 订单明细（快照） ----------
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

-- ---------- 物流单（一单一张） ----------
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

-- ---------- 退货退款申请 ----------
-- 一单同时只能有一个进行中的申请：靠「订单状态 REFUNDING 的条件更新」保证，
-- 所以这里不建 order_id 唯一键（驳回后允许重新申请）。
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

-- ============================================================
-- P5：评价系统
-- ------------------------------------------------------------
-- 设计要点：
--   1. 【评价粒度 = 订单明细】 一个 order_item 只能评一次（unique key）；
--      这样“哪件商品还没评”天然可判定，不会出现重复评价。
--   2. 【只有已完成订单能评】 服务端校验 order.status = COMPLETED。
--   3. 【追评是同一行的追加】 follow_content / follow_time，条件更新防重复追评；
--      追评不改变评分，所以不需要重算商品聚合。
--   4. 【商品聚合写时重算】 新增/删除评价后重算 mall_product 的
--      rating（平均分）/ review_count / good_rate（rating>=4 的占比）。
--      好评率口径：4 星及以上算好评。
--   5. 【匿名展示】 anonymous=1 时对外返回“匿名用户”，不泄露昵称/邮箱。
-- ============================================================

-- ---------- 为 mall_product 补评价聚合字段（幂等加列） ----------
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

-- ---------- 评价主表 ----------
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

-- ---------- 评价图片 ----------
-- type：1 初评图 2 追评图；同一评价两类图分开取。
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

-- 种子商品原本 rating=5.00、review_count=0、good_rate=100.00，与“无评价默认好评”口径一致，无需回填。

-- ---------- 核对 ----------
SELECT '品牌' AS 表, COUNT(*) AS 条数 FROM mall_brand
UNION ALL SELECT '分类', COUNT(*) FROM mall_category
UNION ALL SELECT '商品', COUNT(*) FROM mall_product
UNION ALL SELECT '规格项', COUNT(*) FROM mall_spec
UNION ALL SELECT '规格值', COUNT(*) FROM mall_spec_value
UNION ALL SELECT 'SKU', COUNT(*) FROM mall_sku
UNION ALL SELECT 'SKU关联', COUNT(*) FROM mall_sku_spec
UNION ALL SELECT '推荐位', COUNT(*) FROM mall_banner
UNION ALL SELECT '收藏', COUNT(*) FROM mall_favorite
UNION ALL SELECT '足迹', COUNT(*) FROM mall_footprint
UNION ALL SELECT '购物车行', COUNT(*) FROM mall_cart_item
UNION ALL SELECT '订单', COUNT(*) FROM mall_order
UNION ALL SELECT '订单明细', COUNT(*) FROM mall_order_item
UNION ALL SELECT '物流单', COUNT(*) FROM mall_shipment
UNION ALL SELECT '退款申请', COUNT(*) FROM mall_refund
UNION ALL SELECT '评价', COUNT(*) FROM mall_review
UNION ALL SELECT '评价图', COUNT(*) FROM mall_review_image;
