# 商品业态分类与分区展示 —— 设计文档

- 日期：2026-09-17
- 状态：已确认，实施中
- 目标项目：`C:\Users\1\Desktop\deepseek\superMall`（增量改造，非新建）
- 关联文档：`docs/大纲文件.md`、`docs/前端设计说明.md`

## 一、背景

平台原本只有「外卖食品」一种商品。本次引入第二业态「百货商品」，形成两条并列业态线：

- 用户端首页按业态分区，切换后随机展示该业态商品；
- 商家端上架商品时先定业态，再从该业态下选自定义分类；
- 商家端两类商品分两个页面管理。

改造基于现有代码增量进行，`PublicController` 的随机商品流（`randomOnSale`）已存在，本次为其增加业态维度。

## 二、术语（两层结构）

| 术语 | 字段 | 层级 | 谁定义 |
|------|------|------|--------|
| 业态 | `business_type` | 平台级，固定两个 | 平台（枚举） |
| 分类 | `category` | 商家级，自由命名 | 商家 |

`business_type` 与 `category` 是两层概念，不可混用。

## 三、数据模型（方案 1）

- `category.business_type`（源头）：分类归属某个业态；
- `product.business_type`（冗余）：由所选分类**在服务端派生写入**，用于按业态过滤商品；
- 一致性：保存/更新商品时，服务端用 `category_id` 反查分类，把 `category.business_type`
  写入商品，忽略前端传值。

字段：`business_type TINYINT NOT NULL DEFAULT 1 COMMENT '1外卖食品 2百货商品'`

## 四、业态枚举

`superMall-takeout` 的 `com.supermall.takeout.enumeration.BusinessType`：

```java
FOOD(1, "外卖食品"), GOODS(2, "百货商品");
// getCode / getDesc / isValid(Integer) / ofCode(Integer)
```

## 五、后端改造点（增量）

| 文件 | 改动 |
|------|------|
| `superMall-takeout/.../enumeration/BusinessType.java` | 新增 |
| `entity/Category.java`、`entity/Product.java` | 加 `businessType` |
| `dto/CategoryDTO.java` | 加 `businessType`（@NotNull） |
| `dto/CategoryPageQueryDTO.java` | 加 `businessType`（筛选） |
| `dto/ProductPageQueryDTO.java`、`dto/ProductQueryDTO.java` | 加 `businessType`（筛选） |
| `mapper/CategoryMapper.java` + XML | `countByName` 加业态维度；insert/update/pageQuery 带业态 |
| `mapper/ProductMapper.java` + XML | insert/update 带业态；pageQuery/pageQueryForUser/randomOnSale 按业态过滤 |
| `service/ProductService(Impl)` | 保存/更新时从分类派生业态；`randomFeed` 增加业态参数 |
| `service/impl/CategoryServiceImpl.java` | 保存/更新校验业态；分类重名校验按「商家+业态」 |
| `constant/MessageConstant.java` | 加 `BUSINESS_TYPE_INVALID` |
| `controller/publicapi/PublicController.java` | `productFeed` 增加 `businessType` 参数 |

`ProductDTO` **不加** `businessType` —— 业态由分类派生，前端只传 `categoryId`。

## 六、前端改造点

| 文件 | 改动 |
|------|------|
| `mall-user-web/views/HomeView.vue` | 顶部业态 tab（外卖食品/百货商品），切换后重新随机；feed 带 `businessType` |
| `mall-merchant-web/views/CategoriesView.vue` | 业态 tab，按业态增删改查分类；表单带 `businessType` |
| `mall-merchant-web/views/ProductsView.vue` | 按业态拆两个页面；分类下拉只列本业态分类 |
| `mall-merchant-web/router/index.js` | 商品管理两条路由（food / goods） |
| `mall-merchant-web/App.vue` | 侧边栏商品管理拆两个菜单项 |

## 七、规则与校验

1. 业态入参用 `BusinessType.isValid` 校验，非法值抛业务异常；
2. 商品业态一律由分类派生，前端不可指定；
3. 分类重名判定范围 = 同一商家 + 同一业态；
4. 分类下存在商品时禁止删除（现有逻辑保留）；
5. 商家数据隔离沿用 `BaseContext`，本次不改。

## 八、迁移与种子数据

- `sql/supermall.sql`（全新初始化）：`category`/`product` 加字段与索引；种子补业态；
  新增百货商家（merchant3）与其分类、商品，便于演示。
- `sql/migration-2026-09-17-business-type.sql`（已有库）：`ALTER TABLE` 加字段，
  存量数据按分类回填业态。

## 九、验收标准

1. 首页切换业态 tab，随机商品随之切换；「换一批」结果变化；
2. 商家端分类管理按业态分组，分类只能建在对应业态下；
3. 商家端两个商品页面各自只显示本业态商品；上架时分类下拉只列本业态分类；
4. 新建商品落库后 `product.business_type` 与所选分类一致；
5. 非法 `businessType` 被拒绝。

## 十、本期不做

- 百货的快递物流履约（与外卖共用订单/配送流程）；
- 动态业态配置表（固定枚举，后续真加第三业态再升级）；
- 店铺页（ShopView）内的业态切换；
- 随机展示的性能优化（预生成随机列、缓存等）。
