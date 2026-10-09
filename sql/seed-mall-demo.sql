-- 自动生成，请勿手改：node scripts/seed-mall-demo.mjs
-- 电商演示种子数据：分类 / 品牌 / 商品 / 规格 / SKU / 推荐位
-- 图片来自 frontend/public/seed/（图源：pexels，仅本地演示用）

SET NAMES utf8mb4;

-- 清理上一次的种子数据（只动种子 id 区间，不影响真实数据）
DELETE FROM mall_sku_spec WHERE id BETWEEN 1001 AND 1999;
DELETE FROM mall_sku WHERE id BETWEEN 101 AND 499;
DELETE FROM mall_spec_value WHERE id BETWEEN 1001 AND 1999;
DELETE FROM mall_spec WHERE id BETWEEN 101 AND 399;
DELETE FROM mall_product_image WHERE id BETWEEN 1001 AND 1999;
DELETE FROM mall_product WHERE id BETWEEN 101 AND 199;
DELETE FROM mall_banner WHERE id BETWEEN 101 AND 199;
DELETE FROM mall_brand WHERE id BETWEEN 101 AND 199;
DELETE FROM mall_category WHERE id BETWEEN 101 AND 199;

-- 分类
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (101,0,1,'家用电器','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-101.jpg',1,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (102,101,2,'厨房电器','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-102.jpg',2,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (103,101,2,'清洁电器','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-103.jpg',3,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (104,101,2,'生活电器','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-104.jpg',4,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (105,0,1,'美妆个护','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-105.jpg',5,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (106,105,2,'护肤','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-106.jpg',6,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (107,105,2,'彩妆','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-107.jpg',7,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (108,105,2,'洗护','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-108.jpg',8,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (109,0,1,'家居生活','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-109.jpg',9,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (110,109,2,'家纺','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-110.jpg',10,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (111,109,2,'收纳','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-111.jpg',11,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (112,109,2,'厨具','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-112.jpg',12,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (113,0,1,'运动户外','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-113.jpg',13,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (114,113,2,'健身器材','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-114.jpg',14,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (115,113,2,'户外装备','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-115.jpg',15,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (116,0,1,'母婴玩具','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-116.jpg',16,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (117,116,2,'喂养','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-117.jpg',17,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (118,116,2,'玩具','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-118.jpg',18,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (120,10,3,'耳机音频','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-120.jpg',20,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (121,10,3,'智能穿戴','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-121.jpg',21,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (122,10,3,'电脑外设','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-122.jpg',22,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (123,1,3,'卫衣','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-123.jpg',23,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (124,1,3,'牛仔裤','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-124.jpg',24,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (125,1,3,'箱包','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-125.jpg',25,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (126,1,3,'配饰','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-126.jpg',26,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (127,7,3,'零食','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-127.jpg',27,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES (128,7,3,'茶叶','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-128.jpg',28,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');

-- 已有分类补封面
UPDATE mall_category SET icon='https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-001.jpg' WHERE id=1;
UPDATE mall_category SET icon='https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-007.jpg' WHERE id=7;
UPDATE mall_category SET icon='https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-010.jpg' WHERE id=10;

-- 品牌
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (101,'素野',NULL,'基础款服饰，主打面料与版型',1,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (102,'云岚',NULL,'轻户外与通勤箱包',2,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (103,'沐白',NULL,'家居布艺与床品',3,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (104,'初禾',NULL,'温和配方的个护产品',4,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (105,'拾光',NULL,'精品咖啡与茶饮',5,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (106,'北野',NULL,'户外露营装备',6,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (107,'简厨',NULL,'厨房小家电与厨具',7,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (108,'星芒',NULL,'数码配件与智能穿戴',8,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (109,'轻语',NULL,'家居收纳方案',9,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (110,'悦己',NULL,'彩妆与护肤',10,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (111,'果芽',NULL,'母婴喂养与玩具',11,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (112,'风行者',NULL,'跑步与健身装备',12,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (113,'青谷',NULL,'健康零食与冲饮',13,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (114,'白露',NULL,'香氛与生活电器',14,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES (115,'极地线',NULL,'专业户外服饰',15,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');

-- 商品 / 规格 / SKU
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (101,4,101,'260g 重磅纯棉T恤','厚实不透，不易变形','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-101-1.jpg','260g 重磅纯棉T恤。厚实不透，不易变形。

· 规格：颜色 共 3 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',79.00,89.00,197,853,5.00,1,1,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (101,101,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1001,101,'白',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1002,101,'黑',2,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1003,101,'灰',3,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (101,101,'101-1',79.00,66,284,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1001,101,101,1001);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (102,101,'101-2',84.00,66,284,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1002,102,101,1002);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (103,101,'101-3',89.00,66,284,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1003,103,101,1003);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (102,5,101,'法兰绒磨毛衬衫','秋冬内搭外穿都合适','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-102-1.jpg','法兰绒磨毛衬衫。秋冬内搭外穿都合适。

· 规格：尺码 共 3 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',159.00,179.00,234,6,5.00,1,2,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (102,102,'尺码',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1004,102,'M',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1005,102,'L',2,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1006,102,'XL',3,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (104,102,'102-1',159.00,78,2,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1004,104,102,1004);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (105,102,'102-2',169.00,78,2,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1005,105,102,1005);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (106,102,'102-3',179.00,78,2,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1006,106,102,1006);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (103,6,112,'轻量回弹跑鞋','中底回弹，久站不累','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-103-1.jpg','轻量回弹跑鞋。中底回弹，久站不累。

· 规格：尺码 共 3 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',299.00,339.00,271,59,5.00,1,3,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (103,103,'尺码',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1007,103,'39',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1008,103,'41',2,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1009,103,'43',3,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (107,103,'103-1',299.00,90,20,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1007,107,103,1007);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (108,103,'103-2',319.00,90,20,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1008,108,103,1008);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (109,103,'103-3',339.00,90,20,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1009,109,103,1009);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (104,123,101,'抓绒连帽卫衣','内里抓绒，柔软保暖','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-104-1.jpg','抓绒连帽卫衣。内里抓绒，柔软保暖。

· 规格：尺码 共 3 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',189.00,209.00,68,112,5.00,1,4,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (104,104,'尺码',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1010,104,'M',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1011,104,'L',2,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1012,104,'XL',3,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (110,104,'104-1',189.00,23,37,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1010,110,104,1010);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (111,104,'104-2',199.00,23,37,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1011,111,104,1011);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (112,104,'104-3',209.00,23,37,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1012,112,104,1012);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (105,124,101,'直筒水洗牛仔裤','微弹面料，不勒腿','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-105-1.jpg','直筒水洗牛仔裤。微弹面料，不勒腿。

· 规格：尺码 共 3 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',219.00,239.00,105,165,5.00,1,5,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (105,105,'尺码',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1013,105,'28',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1014,105,'30',2,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1015,105,'32',3,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (113,105,'105-1',219.00,35,55,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1013,113,105,1013);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (114,105,'105-2',229.00,35,55,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1014,114,105,1014);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (115,105,'105-3',239.00,35,55,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1015,115,105,1015);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (106,125,102,'通勤双肩背包','15.6 寸电脑隔层','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-106-1.jpg','通勤双肩背包。15.6 寸电脑隔层。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',199.00,229.00,142,218,5.00,1,6,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (106,106,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1016,106,'黑',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1017,106,'藏青',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (116,106,'106-1',199.00,71,109,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1016,116,106,1016);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (117,106,'106-2',229.00,71,109,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1017,117,106,1017);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (107,125,102,'帆布托特包','加厚帆布，能装耐用','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-107-1.jpg','帆布托特包。加厚帆布，能装耐用。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',89.00,99.00,179,271,5.00,1,7,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (107,107,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1018,107,'米白',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1019,107,'墨绿',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (118,107,'107-1',89.00,90,136,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1018,118,107,1018);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (119,107,'107-2',99.00,90,136,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1019,119,107,1019);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (108,126,101,'纯羊毛针织围巾','亲肤不扎，保暖好搭','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-108-1.jpg','纯羊毛针织围巾。亲肤不扎，保暖好搭。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',129.00,149.00,216,324,5.00,1,8,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (108,108,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1020,108,'驼色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1021,108,'深灰',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (120,108,'108-1',129.00,108,162,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1020,120,108,1020);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (121,108,'108-2',149.00,108,162,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1021,121,108,1021);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (109,126,101,'头层牛皮腰带','自动扣，宽窄两用','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-109-1.jpg','头层牛皮腰带。自动扣，宽窄两用。

· 规格：尺码 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',99.00,119.00,253,377,5.00,1,9,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (109,109,'尺码',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1022,109,'105cm',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1023,109,'115cm',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (122,109,'109-1',99.00,127,189,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1022,122,109,1022);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (123,109,'109-2',119.00,127,189,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1023,123,109,1023);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (110,126,101,'纯棉棒球帽','可调节，男女同款','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-110-1.jpg','纯棉棒球帽。可调节，男女同款。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',59.00,69.00,290,430,5.00,1,10,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (110,110,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1024,110,'黑',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1025,110,'米白',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (124,110,'110-1',59.00,145,215,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1024,124,110,1024);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (125,110,'110-2',69.00,145,215,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1025,125,110,1025);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (111,123,115,'三合一冲锋衣','防风防泼水，可拆内胆','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-111-1.jpg','三合一冲锋衣。防风防泼水，可拆内胆。

· 规格：尺码 共 3 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',499.00,559.00,87,483,5.00,1,11,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (111,111,'尺码',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1026,111,'M',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1027,111,'L',2,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1028,111,'XL',3,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (126,111,'111-1',499.00,29,161,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1026,126,111,1026);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (127,111,'111-2',529.00,29,161,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1027,127,111,1027);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (128,111,'111-3',559.00,29,161,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1028,128,111,1028);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (112,123,101,'宽松针织开衫','慵懒版型，空调房必备','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-112-1.jpg','宽松针织开衫。慵懒版型，空调房必备。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',169.00,189.00,124,536,5.00,1,12,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (112,112,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1029,112,'燕麦',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1030,112,'雾蓝',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (129,112,'112-1',169.00,62,268,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1029,129,112,1029);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (130,112,'112-2',189.00,62,268,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1030,130,112,1030);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (113,120,108,'主动降噪蓝牙耳机','通勤降噪，单次 8 小时','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-113-1.jpg','主动降噪蓝牙耳机。通勤降噪，单次 8 小时。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',399.00,449.00,161,589,5.00,1,13,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (113,113,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1031,113,'白',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1032,113,'黑',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (131,113,'113-1',399.00,81,295,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1031,131,113,1031);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (132,113,'113-2',449.00,81,295,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1032,132,113,1032);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (114,120,108,'桌面蓝牙音箱','木质腔体，人声通透','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-114-1.jpg','桌面蓝牙音箱。木质腔体，人声通透。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',269.00,299.00,198,642,5.00,1,14,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (114,114,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1033,114,'木色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1034,114,'黑',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (133,114,'114-1',269.00,99,321,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1033,133,114,1033);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (134,114,'114-2',299.00,99,321,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1034,134,114,1034);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (115,121,108,'全天候智能手表','心率血氧，14 天续航','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-115-1.jpg','全天候智能手表。心率血氧，14 天续航。

· 规格：表带 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',599.00,699.00,235,695,5.00,1,15,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (115,115,'表带',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1035,115,'硅胶',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1036,115,'编织',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (135,115,'115-1',599.00,118,348,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1035,135,115,1035);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (136,115,'115-2',699.00,118,348,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1036,136,115,1036);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (116,122,108,'87 键机械键盘','热插拔轴体，紧凑布局','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-116-1.jpg','87 键机械键盘。热插拔轴体，紧凑布局。

· 规格：轴体 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',329.00,369.00,272,748,5.00,1,16,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (116,116,'轴体',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1037,116,'红轴',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1038,116,'茶轴',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (137,116,'116-1',329.00,136,374,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1037,137,116,1037);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (138,116,'116-2',369.00,136,374,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1038,138,116,1038);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (117,122,108,'无线静音鼠标','双模连接，静音微动','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-117-1.jpg','无线静音鼠标。双模连接，静音微动。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',89.00,99.00,69,801,5.00,1,17,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (117,117,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1039,117,'灰',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1040,117,'白',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (139,117,'117-1',89.00,35,401,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1039,139,117,1039);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (140,117,'117-2',99.00,35,401,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1040,140,117,1040);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (118,10,108,'20000mAh 移动电源','双向快充，可上飞机','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-118-1.jpg','20000mAh 移动电源。双向快充，可上飞机。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',149.00,169.00,106,854,5.00,1,18,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (118,118,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1041,118,'黑',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1042,118,'白',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (141,118,'118-1',149.00,53,427,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1041,141,118,1041);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (142,118,'118-2',169.00,53,427,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1042,142,118,1042);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (119,11,108,'65W 氮化镓充电器','一个头喂饱手机电脑','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-119-1.jpg','65W 氮化镓充电器。一个头喂饱手机电脑。

· 规格：接口 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',129.00,149.00,143,7,5.00,1,19,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (119,119,'接口',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1043,119,'双 C',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1044,119,'C+A',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (143,119,'119-1',129.00,72,4,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1043,143,119,1043);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (144,119,'119-2',149.00,72,4,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1044,144,119,1044);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (120,10,108,'磁吸防摔手机壳','镜头全包，手感细腻','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-120-1.jpg','磁吸防摔手机壳。镜头全包，手感细腻。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',49.00,59.00,180,60,5.00,1,20,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (120,120,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1045,120,'透明',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1046,120,'黑',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (145,120,'120-1',49.00,90,30,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1045,145,120,1045);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (146,120,'120-2',59.00,90,30,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1046,146,120,1046);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (121,102,107,'IH 电饭煲 4L','厚釜内胆，米饭更饱满','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-121-1.jpg','IH 电饭煲 4L。厚釜内胆，米饭更饱满。

· 规格：容量 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',399.00,459.00,217,113,5.00,1,21,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (121,121,'容量',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1047,121,'3L',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1048,121,'4L',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (147,121,'121-1',399.00,109,57,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1047,147,121,1047);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (148,121,'121-2',459.00,109,57,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1048,148,121,1048);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (122,102,107,'可视空气炸锅 5L','少油少烟，一锅多用','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-122-1.jpg','可视空气炸锅 5L。少油少烟，一锅多用。

· 规格：容量 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',329.00,379.00,254,166,5.00,1,22,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (122,122,'容量',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1049,122,'4L',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1050,122,'5L',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (149,122,'122-1',329.00,127,83,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1049,149,122,1049);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (150,122,'122-2',379.00,127,83,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1050,150,122,1050);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (123,102,107,'破壁料理机','八叶刀头，细腻无渣','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-123-1.jpg','破壁料理机。八叶刀头，细腻无渣。

· 规格：容量 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',459.00,529.00,291,219,5.00,1,23,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (123,123,'容量',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1051,123,'1.2L',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1052,123,'1.75L',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (151,123,'123-1',459.00,146,110,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1051,151,123,1051);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (152,123,'123-2',529.00,146,110,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1052,152,123,1052);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (124,102,107,'恒温电热水壶','六段控温，烧水更快','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-124-1.jpg','恒温电热水壶。六段控温，烧水更快。

· 规格：容量 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',179.00,199.00,88,272,5.00,1,24,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (124,124,'容量',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1053,124,'1.5L',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1054,124,'1.7L',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (153,124,'124-1',179.00,44,136,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1053,153,124,1053);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (154,124,'124-2',199.00,44,136,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1054,154,124,1054);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (125,102,107,'半自动意式咖啡机','20Bar 萃取，家用足够','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-125-1.jpg','半自动意式咖啡机。20Bar 萃取，家用足够。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',899.00,999.00,125,325,5.00,1,25,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (125,125,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1055,125,'银',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1056,125,'黑',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (155,125,'125-1',899.00,63,163,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1055,155,125,1055);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (156,125,'125-2',999.00,63,163,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1056,156,125,1056);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (126,103,107,'激光导航扫地机器人','边扫边拖，自动回充','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-126-1.jpg','激光导航扫地机器人。边扫边拖，自动回充。

· 规格：版本 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',1299.00,1499.00,162,378,5.00,1,26,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (126,126,'版本',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1057,126,'标准版',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1058,126,'自集尘',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (157,126,'126-1',1299.00,81,189,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1057,157,126,1057);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (158,126,'126-2',1499.00,81,189,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1058,158,126,1058);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (127,103,107,'轻量无线吸尘器','整机 1.4kg，女生也能举','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-127-1.jpg','轻量无线吸尘器。整机 1.4kg，女生也能举。

· 规格：版本 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',799.00,899.00,199,431,5.00,1,27,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (127,127,'版本',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1059,127,'标准',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1060,127,'除螨套装',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (159,127,'127-1',799.00,100,216,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1059,159,127,1059);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (160,127,'127-2',899.00,100,216,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1060,160,127,1060);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (128,104,114,'上加水加湿器','大雾量，静音夜用','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-128-1.jpg','上加水加湿器。大雾量，静音夜用。

· 规格：容量 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',199.00,229.00,236,484,5.00,1,28,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (128,128,'容量',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1061,128,'4L',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1062,128,'6L',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (161,128,'128-1',199.00,118,242,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1061,161,128,1061);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (162,128,'128-2',229.00,118,242,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1062,162,128,1062);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (129,104,114,'落地循环扇','直流电机，风感柔和','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-129-1.jpg','落地循环扇。直流电机，风感柔和。

· 规格：档位 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',299.00,339.00,273,537,5.00,1,29,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (129,129,'档位',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1063,129,'8 档',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1064,129,'12 档',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (163,129,'129-1',299.00,137,269,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1063,163,129,1063);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (164,129,'129-2',339.00,137,269,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1064,164,129,1064);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (130,106,110,'神经酰胺保湿面霜','修护屏障，不闷痘','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-130-1.jpg','神经酰胺保湿面霜。修护屏障，不闷痘。

· 规格：规格 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',159.00,179.00,70,590,5.00,1,30,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (130,130,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1065,130,'50ml',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1066,130,'80ml',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (165,130,'130-1',159.00,35,295,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1065,165,130,1065);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (166,130,'130-2',179.00,35,295,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1066,166,130,1066);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (131,106,110,'烟酰胺精华液','提亮肤色，温和不刺激','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-131-1.jpg','烟酰胺精华液。提亮肤色，温和不刺激。

· 规格：规格 共 1 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',199.00,219.00,107,643,5.00,1,31,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (131,131,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1067,131,'30ml',1,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (167,131,'131-1',199.00,107,643,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1067,167,131,1067);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (132,106,104,'多肽紧致眼霜','轻薄好推，不长脂肪粒','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-132-1.jpg','多肽紧致眼霜。轻薄好推，不长脂肪粒。

· 规格：规格 共 1 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',179.00,199.00,144,696,5.00,1,32,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (132,132,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1068,132,'15g',1,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (168,132,'132-1',179.00,144,696,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1068,168,132,1068);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (133,106,104,'氨基酸洁面乳','洗后不紧绷','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-133-1.jpg','氨基酸洁面乳。洗后不紧绷。

· 规格：规格 共 1 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',69.00,79.00,181,749,5.00,1,33,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (133,133,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1069,133,'120g',1,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (169,133,'133-1',69.00,181,749,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1069,169,133,1069);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (134,107,110,'丝绒雾面口红','显白不拔干','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-134-1.jpg','丝绒雾面口红。显白不拔干。

· 规格：色号 共 3 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',129.00,139.00,218,802,5.00,1,34,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (134,134,'色号',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1070,134,'枫叶红',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1071,134,'豆沙',2,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1072,134,'蜜桃',3,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (170,134,'134-1',129.00,73,267,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1070,170,134,1070);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (171,134,'134-2',134.00,73,267,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1071,171,134,1071);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (172,134,'134-3',139.00,73,267,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1072,172,134,1072);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (135,107,110,'持妆粉底液','轻薄服帖，控油持妆','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-135-1.jpg','持妆粉底液。轻薄服帖，控油持妆。

· 规格：色号 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',189.00,209.00,255,855,5.00,1,35,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (135,135,'色号',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1073,135,'自然色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1074,135,'象牙白',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (173,135,'135-1',189.00,128,428,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1073,173,135,1073);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (174,135,'135-2',209.00,128,428,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1074,174,135,1074);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (136,107,110,'大地色眼影盘','哑光珠光一盘搞定','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-136-1.jpg','大地色眼影盘。哑光珠光一盘搞定。

· 规格：色号 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',139.00,149.00,292,8,5.00,1,36,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (136,136,'色号',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1075,136,'大地色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1076,136,'玫瑰色',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (175,136,'136-1',139.00,146,4,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1075,175,136,1075);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (176,136,'136-2',149.00,146,4,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1076,176,136,1076);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (137,108,104,'无硅油洗发水','控油蓬松，温和清洁','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-137-1.jpg','无硅油洗发水。控油蓬松，温和清洁。

· 规格：规格 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',79.00,89.00,89,61,5.00,1,37,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (137,137,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1077,137,'500ml',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1078,137,'750ml',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (177,137,'137-1',79.00,45,31,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1077,177,137,1077);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (178,137,'137-2',89.00,45,31,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1078,178,137,1078);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (138,108,104,'氨基酸沐浴露','泡沫绵密，洗完不假滑','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-138-1.jpg','氨基酸沐浴露。泡沫绵密，洗完不假滑。

· 规格：香型 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',69.00,79.00,126,114,5.00,1,38,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (138,138,'香型',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1079,138,'白茶',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1080,138,'雪松',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (179,138,'138-1',69.00,63,57,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1079,179,138,1079);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (180,138,'138-2',79.00,63,57,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1080,180,138,1080);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (139,108,104,'护龈牙膏三支装','清新口气，呵护牙龈','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-139-1.jpg','护龈牙膏三支装。清新口气，呵护牙龈。

· 规格：规格 共 1 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',49.00,55.00,163,167,5.00,1,39,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (139,139,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1081,139,'3 支装',1,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (181,139,'139-1',49.00,163,167,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1081,181,139,1081);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (140,110,103,'60 支长绒棉四件套','亲肤透气，越洗越软','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-140-1.jpg','60 支长绒棉四件套。亲肤透气，越洗越软。

· 规格：尺寸 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',399.00,469.00,200,220,5.00,1,40,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (140,140,'尺寸',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1082,140,'1.5m 床',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1083,140,'1.8m 床',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (182,140,'140-1',399.00,100,110,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1082,182,140,1082);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (183,140,'140-2',469.00,100,110,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1083,183,140,1083);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (141,110,103,'慢回弹记忆枕','护颈曲度，翻身不塌','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-141-1.jpg','慢回弹记忆枕。护颈曲度，翻身不塌。

· 规格：高度 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',169.00,189.00,237,273,5.00,1,41,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (141,141,'高度',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1084,141,'低枕',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1085,141,'高枕',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (184,141,'141-1',169.00,119,137,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1084,184,141,1084);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (185,141,'141-2',189.00,119,137,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1085,185,141,1085);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (142,110,103,'加厚羊毛毯','午睡盖毯，柔软不扎','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-142-1.jpg','加厚羊毛毯。午睡盖毯，柔软不扎。

· 规格：尺寸 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',199.00,229.00,274,326,5.00,1,42,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (142,142,'尺寸',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1086,142,'150×200',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1087,142,'200×230',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (186,142,'142-1',199.00,137,163,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1086,186,142,1086);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (187,142,'142-2',229.00,137,163,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1087,187,142,1087);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (143,110,103,'纯棉强吸水毛巾','A 类标准，越洗越软','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-143-1.jpg','纯棉强吸水毛巾。A 类标准，越洗越软。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',39.00,45.00,71,379,5.00,1,43,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (143,143,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1088,143,'灰',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1089,143,'米白',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (188,143,'143-1',39.00,36,190,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1088,188,143,1088);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (189,143,'143-2',45.00,36,190,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1089,189,143,1089);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (144,111,109,'可折叠收纳箱','带轮可叠，换季好帮手','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-144-1.jpg','可折叠收纳箱。带轮可叠，换季好帮手。

· 规格：容量 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',89.00,109.00,108,432,5.00,1,44,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (144,144,'容量',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1090,144,'45L',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1091,144,'66L',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (190,144,'144-1',89.00,54,216,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1090,190,144,1090);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (191,144,'144-2',109.00,54,216,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1091,191,144,1091);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (145,111,109,'防滑植绒衣架 20 只','省空间，不滑落','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-145-1.jpg','防滑植绒衣架 20 只。省空间，不滑落。

· 规格：规格 共 1 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',45.00,55.00,145,485,5.00,1,45,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (145,145,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1092,145,'20 只',1,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (192,145,'145-1',45.00,145,485,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1092,192,145,1092);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (146,112,107,'麦饭石不粘炒锅','少油不粘，燃气电磁通用','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-146-1.jpg','麦饭石不粘炒锅。少油不粘，燃气电磁通用。

· 规格：口径 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',229.00,259.00,182,538,5.00,1,46,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (146,146,'口径',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1093,146,'28cm',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1094,146,'30cm',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (193,146,'146-1',229.00,91,269,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1093,193,146,1093);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (194,146,'146-2',259.00,91,269,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1094,194,146,1094);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (147,112,107,'德国钢厨房刀具五件套','锋利耐用，带刀座','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-147-1.jpg','德国钢厨房刀具五件套。锋利耐用，带刀座。

· 规格：套装 共 1 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',269.00,299.00,219,591,5.00,1,47,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (147,147,'套装',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1095,147,'五件套',1,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (195,147,'147-1',269.00,219,591,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1095,195,147,1095);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (148,112,107,'316 不锈钢保温杯','12 小时保温','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-148-1.jpg','316 不锈钢保温杯。12 小时保温。

· 规格：容量 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',129.00,149.00,256,644,5.00,1,48,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (148,148,'容量',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1096,148,'400ml',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1097,148,'500ml',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (196,148,'148-1',129.00,128,322,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1096,196,148,1096);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (197,148,'148-2',149.00,128,322,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1097,197,148,1097);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (149,114,112,'TPE 双面防滑瑜伽垫','加厚 8mm，回弹好','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-149-1.jpg','TPE 双面防滑瑜伽垫。加厚 8mm，回弹好。

· 规格：颜色 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',129.00,149.00,293,697,5.00,1,49,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (149,149,'颜色',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1098,149,'紫',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1099,149,'灰蓝',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (198,149,'149-1',129.00,147,349,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1098,198,149,1098);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (199,149,'149-2',149.00,147,349,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1099,199,149,1099);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (150,114,112,'可调节包胶哑铃','一对到手，重量可加','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-150-1.jpg','可调节包胶哑铃。一对到手，重量可加。

· 规格：重量 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',199.00,239.00,90,750,5.00,1,50,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (150,150,'重量',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1100,150,'10kg',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1101,150,'20kg',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (200,150,'150-1',199.00,45,375,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1100,200,150,1100);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (201,150,'150-2',239.00,45,375,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1101,201,150,1101);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (151,114,112,'乳胶弹力带套装','五个阻力级别','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-151-1.jpg','乳胶弹力带套装。五个阻力级别。

· 规格：套装 共 1 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',59.00,69.00,127,803,5.00,1,51,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (151,151,'套装',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1102,151,'五件套',1,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (202,151,'151-1',59.00,127,803,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1102,202,151,1102);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (152,114,112,'钢丝竞速跳绳','轴承顺滑，可调长度','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-152-1.jpg','钢丝竞速跳绳。轴承顺滑，可调长度。

· 规格：手柄 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',39.00,45.00,164,856,5.00,1,52,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (152,152,'手柄',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1103,152,'泡沫',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1104,152,'合金',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (203,152,'152-1',39.00,82,428,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1103,203,152,1103);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (204,152,'152-2',45.00,82,428,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1104,204,152,1104);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (153,115,106,'自动速开帐篷 3-4 人','一秒开合，防泼水','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-153-1.jpg','自动速开帐篷 3-4 人。一秒开合，防泼水。

· 规格：规格 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',399.00,459.00,201,9,5.00,1,53,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (153,153,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1105,153,'3 人',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1106,153,'4 人',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (205,153,'153-1',399.00,101,5,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1105,205,153,1105);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (206,153,'153-2',459.00,101,5,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1106,206,153,1106);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (154,115,106,'轻量登山包 40L','背负透气，可挂登山杖','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-154-1.jpg','轻量登山包 40L。背负透气，可挂登山杖。

· 规格：容量 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',329.00,369.00,238,62,5.00,1,54,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (154,154,'容量',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1107,154,'35L',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1108,154,'40L',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (207,154,'154-1',329.00,119,31,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1107,207,154,1107);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (208,154,'154-2',369.00,119,31,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1108,208,154,1108);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (155,115,106,'信封式睡袋','可拼接，四季可用','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-155-1.jpg','信封式睡袋。可拼接，四季可用。

· 规格：温标 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',199.00,229.00,275,115,5.00,1,55,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (155,155,'温标',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1109,155,'5℃',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1110,155,'0℃',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (209,155,'155-1',199.00,138,58,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1109,209,155,1109);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (210,155,'155-2',229.00,138,58,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1110,210,155,1110);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (156,117,111,'宽口径玻璃奶瓶','耐高温，防胀气','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-156-1.jpg','宽口径玻璃奶瓶。耐高温，防胀气。

· 规格：容量 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',89.00,99.00,72,168,5.00,1,56,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (156,156,'容量',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1111,156,'160ml',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1112,156,'240ml',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (211,156,'156-1',89.00,36,84,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1111,211,156,1111);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (212,156,'156-2',99.00,36,84,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1112,212,156,1112);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (157,117,111,'蒸煮搅拌辅食机','一键做辅食','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-157-1.jpg','蒸煮搅拌辅食机。一键做辅食。

· 规格：容量 共 1 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',299.00,329.00,109,221,5.00,1,57,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (157,157,'容量',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1113,157,'300ml',1,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (213,157,'157-1',299.00,109,221,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1113,213,157,1113);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (158,118,111,'大颗粒益智积木','圆角打磨，安全无异味','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-158-1.jpg','大颗粒益智积木。圆角打磨，安全无异味。

· 规格：规格 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',129.00,149.00,146,274,5.00,1,58,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (158,158,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1114,158,'80 粒',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1115,158,'160 粒',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (214,158,'158-1',129.00,73,137,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1114,214,158,1114);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (215,158,'158-2',149.00,73,137,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1115,215,158,1115);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (159,118,111,'安抚毛绒玩具','可机洗，柔软亲肤','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-159-1.jpg','安抚毛绒玩具。可机洗，柔软亲肤。

· 规格：款式 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',79.00,89.00,183,327,5.00,1,59,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (159,159,'款式',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1116,159,'小熊',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1117,159,'兔子',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (216,159,'159-1',79.00,92,164,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1116,216,159,1116);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (217,159,'159-2',89.00,92,164,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1117,217,159,1117);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (160,118,111,'儿童进阶拼图','分阶难度，锻炼专注','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-160-1.jpg','儿童进阶拼图。分阶难度，锻炼专注。

· 规格：阶段 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',59.00,69.00,220,380,5.00,1,60,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (160,160,'阶段',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1118,160,'2 阶',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1119,160,'3 阶',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (218,160,'160-1',59.00,110,190,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1118,218,160,1118);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (219,160,'160-2',69.00,110,190,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1119,219,160,1119);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (161,9,105,'挂耳咖啡 10 片装','现磨锁鲜，办公室友好','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-161-1.jpg','挂耳咖啡 10 片装。现磨锁鲜，办公室友好。

· 规格：烘焙度 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',59.00,69.00,257,433,5.00,1,61,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (161,161,'烘焙度',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1120,161,'中烘',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1121,161,'深烘',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (220,161,'161-1',59.00,129,217,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1120,220,161,1120);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (221,161,'161-2',69.00,129,217,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1121,221,161,1121);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (162,8,105,'冷萃咖啡液 12 条','0 糖 0 脂，加奶即饮','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-162-1.jpg','冷萃咖啡液 12 条。0 糖 0 脂，加奶即饮。

· 规格：口味 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',79.00,89.00,294,486,5.00,1,62,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (162,162,'口味',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1122,162,'原味',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1123,162,'榛果',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (222,162,'162-1',79.00,147,243,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1122,222,162,1122);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (223,162,'162-2',89.00,147,243,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1123,223,162,1123);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (163,128,113,'明前龙井茶罐装','豆香明显，回甘持久','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-163-1.jpg','明前龙井茶罐装。豆香明显，回甘持久。

· 规格：规格 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',128.00,168.00,91,539,5.00,1,63,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (163,163,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1124,163,'50g',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1125,163,'100g',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (224,163,'163-1',128.00,46,270,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1124,224,163,1124);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (225,163,'163-2',168.00,46,270,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1125,225,163,1125);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (164,127,113,'每日坚果混合装','7 种果仁，独立小包','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-164-1.jpg','每日坚果混合装。7 种果仁，独立小包。

· 规格：规格 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',69.00,79.00,128,592,5.00,1,64,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (164,164,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1126,164,'15 包',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1127,164,'30 包',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (226,164,'164-1',69.00,64,296,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1126,226,164,1126);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (227,164,'164-2',79.00,64,296,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1127,227,164,1127);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (165,127,113,'低糖全麦饼干','粗粮饱腹，扛饿不腻','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-165-1.jpg','低糖全麦饼干。粗粮饱腹，扛饿不腻。

· 规格：规格 共 1 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',39.00,45.00,165,645,5.00,1,65,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (165,165,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1128,165,'400g',1,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (228,165,'165-1',39.00,165,645,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1128,228,165,1128);
INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES (166,8,105,'冻干柠檬蜂蜜茶','冷热皆宜，果香清爽','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-166-1.jpg','冻干柠檬蜂蜜茶。冷热皆宜，果香清爽。

· 规格：规格 共 2 种可选
· 发货：下单后 48 小时内发出，支持 7 天无理由退货
· 售后：退款进度可在「我的订单」里随时查看',49.00,55.00,202,698,5.00,1,66,'2026-09-20 10:00:00','2026-09-20 10:00:00',0,100.00);
INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (166,166,'规格',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1129,166,'10 条',1,'2026-09-20 10:00:00');
INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (1130,166,'20 条',2,'2026-09-20 10:00:00');
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (229,166,'166-1',49.00,101,349,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1129,229,166,1129);
INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES (230,166,'166-2',55.00,101,349,NULL,1,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (1130,230,166,1130);

-- 商品图集
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1001,101,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-101-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1002,101,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-101-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1003,102,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-102-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1004,102,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-102-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1005,103,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-103-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1006,103,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-103-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1007,104,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-104-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1008,104,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-104-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1009,105,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-105-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1010,105,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-105-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1011,106,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-106-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1012,106,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-106-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1013,107,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-107-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1014,107,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-107-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1015,108,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-108-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1016,108,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-108-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1017,109,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-109-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1018,109,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-109-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1019,110,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-110-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1020,110,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-110-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1021,111,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-111-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1022,111,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-111-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1023,112,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-112-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1024,112,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-112-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1025,113,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-113-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1026,113,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-113-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1027,114,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-114-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1028,114,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-114-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1029,115,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-115-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1030,115,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-115-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1031,116,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-116-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1032,116,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-116-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1033,117,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-117-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1034,117,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-117-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1035,118,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-118-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1036,118,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-118-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1037,119,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-119-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1038,119,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-119-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1039,120,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-120-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1040,120,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-120-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1041,121,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-121-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1042,121,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-121-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1043,122,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-122-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1044,122,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-122-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1045,123,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-123-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1046,123,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-123-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1047,124,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-124-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1048,124,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-124-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1049,125,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-125-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1050,125,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-125-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1051,126,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-126-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1052,126,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-126-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1053,127,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-127-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1054,127,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-127-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1055,128,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-128-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1056,128,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-128-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1057,129,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-129-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1058,129,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-129-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1059,130,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-130-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1060,130,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-130-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1061,131,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-131-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1062,131,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-131-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1063,132,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-132-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1064,132,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-132-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1065,133,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-133-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1066,133,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-133-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1067,134,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-134-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1068,134,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-134-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1069,135,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-135-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1070,135,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-135-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1071,136,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-136-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1072,136,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-136-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1073,137,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-137-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1074,137,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-137-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1075,138,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-138-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1076,138,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-138-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1077,139,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-139-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1078,139,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-139-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1079,140,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-140-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1080,140,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-140-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1081,141,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-141-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1082,141,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-141-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1083,142,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-142-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1084,142,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-142-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1085,143,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-143-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1086,143,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-143-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1087,144,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-144-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1088,144,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-144-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1089,145,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-145-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1090,145,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-145-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1091,146,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-146-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1092,146,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-146-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1093,147,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-147-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1094,147,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-147-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1095,148,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-148-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1096,148,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-148-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1097,149,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-149-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1098,149,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-149-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1099,150,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-150-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1100,150,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-150-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1101,151,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-151-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1102,151,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-151-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1103,152,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-152-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1104,152,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-152-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1105,153,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-153-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1106,153,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-153-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1107,154,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-154-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1108,154,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-154-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1109,155,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-155-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1110,155,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-155-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1111,156,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-156-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1112,156,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-156-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1113,157,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-157-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1114,157,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-157-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1115,158,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-158-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1116,158,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-158-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1117,159,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-159-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1118,159,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-159-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1119,160,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-160-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1120,160,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-160-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1121,161,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-161-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1122,161,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-161-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1123,162,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-162-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1124,162,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-162-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1125,163,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-163-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1126,163,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-163-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1127,164,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-164-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1128,164,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-164-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1129,165,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-165-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1130,165,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-165-2.jpg',2,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1131,166,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-166-1.jpg',1,'2026-09-20 10:00:00');
INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES (1132,166,'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/p-166-2.jpg',2,'2026-09-20 10:00:00');

-- 推荐位（position 1 轮播 / 2 金刚区，全部上架）
INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES (101,'秋冬新品 · 全场包邮','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/b-101.jpg',1,2,'1',1,1,NULL,NULL,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES (102,'厨房小家电 · 到手价直降','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/b-102.jpg',1,2,'102',2,1,NULL,NULL,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES (103,'露营季 · 装备一站备齐','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/b-103.jpg',1,2,'115',3,1,NULL,NULL,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES (104,'护肤专场 · 温和不刺激','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/b-104.jpg',1,2,'106',4,1,NULL,NULL,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES (105,'服饰鞋包','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/s-01.jpg',2,2,'1',5,1,NULL,NULL,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES (106,'家用电器','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/s-02.jpg',2,2,'101',6,1,NULL,NULL,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES (107,'数码配件','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/s-03.jpg',2,2,'10',7,1,NULL,NULL,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES (108,'美妆个护','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/s-04.jpg',2,2,'105',8,1,NULL,NULL,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES (109,'家居生活','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/s-05.jpg',2,2,'109',9,1,NULL,NULL,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES (110,'运动户外','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/s-06.jpg',2,2,'113',10,1,NULL,NULL,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES (111,'食品饮料','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/s-07.jpg',2,2,'7',11,1,NULL,NULL,'2026-09-20 10:00:00','2026-09-20 10:00:00');
INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES (112,'母婴玩具','https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/s-08.jpg',2,2,'116',12,1,NULL,NULL,'2026-09-20 10:00:00','2026-09-20 10:00:00');

