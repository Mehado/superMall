# superMall 在线外卖商城

> 一个支持 **多商家 / 多用户 / 配送员** 的在线外卖商城后端。
> 平台包含两个业态：**外卖食品** 与 **百货商品**。
> 前后端分离：Spring Boot 4 后端 + 三个 Vue3 前端（用户端 / 商家端 / 配送端）。

---

## 一、技术栈

| 层次 | 选型 |
| --- | --- |
| 语言 / 构建 | Java 21、Maven 多模块 |
| 框架 | Spring Boot 4.1.1、Spring MVC、MyBatis、PageHelper |
| 数据库 | MySQL 8 + Druid 连接池 |
| 缓存 | Redis（Cache-Aside、Lua 原子脚本、限流、幂等、库存） |
| 消息队列 | RabbitMQ（延迟队列 / 死信队列 / 手动 ACK / 生产者确认） |
| 鉴权 | JWT（三个端各自独立密钥）+ BCrypt |
| 其他 | AOP（防重复提交 / 限流）、WebSocket、阿里云 OSS（商品图片）、springdoc + Knife4j、Logback + traceId、jasypt 配置加密 |
| 测试 | JUnit 6 + Mockito + AssertJ + 并发测试；HTTP 压测脚本（Node.js，零依赖） |

---

## 二、模块结构

```
superMall
├── superMall-common     ★ 唯一共享模块：Result / 异常 / 常量 / JWT / 上下文 / Jackson / OSS + 共享模型（User / Address / 登录注册 DTO·VO / 验证码 / 邮箱密码校验）
├── superMall-takeout    ★ 外卖服务 :8080（用户/商家/骑手 三端）
├── superMall-mall       ★ 电商服务 :8081（买家 /api/mall/user、目录 /api/mall/catalog、运营 /api/mall/admin）
│                        —— 独立部署，与外卖互不影响；P0~P5 + P7 已完成（商品/浏览/购物车/订单/评价/运营后台），P6 营销暂缓
├── frontend             六端合一前端 :5178（外卖用户/商家/骑手/平台运营 + 电商买家 + 运营后台）
├── scripts/             接口/文档一致性校验脚本 + HTTP 压测脚本
├── docs/                需求、大纲、接口文档、电商板块说明、设计文档、截图
└── sql/                 建库建表 + 初始化数据 + 迁移脚本（supermall.sql=外卖、mall.sql=电商）
```

两个可独立部署的服务：

| 服务 | 端口 | 启动类 | 说明 |
| --- | --- | --- | --- |
| `superMall-takeout` | 8080 | `com.supermall.takeout.TakeoutApplication` | 外卖：点餐 + 骑手配送 |
| `superMall-mall` | 8081 | `com.supermall.mall.MallApplication` | 电商：商品 + 物流（P0~P5 + P7 + 商品详情缓存三防已完成：认证/商品/浏览/购物车地址/订单/评价/运营后台/Redis 缓存热点商品） |

两者只共享 `superMall-common` 一个库（横切能力 + `User`/`Address` 等共享模型），以及 `sys_user` / `address` 两张表；
各自业务代码与模型完全自持（`com.supermall.takeout.*` / `com.supermall.mall.*`），
其余全部隔离（表前缀 `mall_`、Redis 前缀 `mall:`、MQ 前缀 `mall.`）。
详见 [`docs/电商板块.md`](docs/电商板块.md)。

---

## 三、快速开始

### 1. 准备中间件

| 组件 | 地址 | 凭据 | 说明 |
| --- | --- | --- | --- |
| MySQL | `localhost:3306` | `root` / `ENC(...)` 已加密 | 库存、订单、用户等 |
| Redis | `localhost:6379` | 密码 `ENC(...)` 已加密，db 0 | 库存、幂等、缓存、限流 |
| RabbitMQ | `localhost:5672` | `ENC(...)` 已加密 | 延迟关单、异步统计 |

> **凭据已用 jasypt 加密写入配置文件**，启动时唯一需要提供的是主密码环境变量：
>
> ```bash
> # Windows（用户级，永久生效，设置后需重开终端）
> setx JASYPT_ENCRYPTOR_PASSWORD "你的主密码"
> # Linux / macOS
> export JASYPT_ENCRYPTOR_PASSWORD="你的主密码"
> ```
>
> 主机 / 端口 / 库名等非敏感项仍可用环境变量覆盖：
> `MYSQL_HOST / MYSQL_PORT / MYSQL_DATABASE`、
> `REDIS_HOST / REDIS_PORT / REDIS_DATABASE`、
> `RABBITMQ_HOST / RABBITMQ_PORT`、
> `OSS_ENDPOINT / OSS_BUCKET`

### 2. 初始化数据库

```bash
# 全新安装
mysql -uroot -p < sql/supermall.sql
```

已有库升级（本项目迭代产生的迁移脚本）：

```bash
# 商品业态（业务类型）
mysql -uroot -p supermall < sql/migration-2026-09-17-business-type.sql
# 商家营业状态
mysql -uroot -p supermall < sql/migration-2026-09-17-merchant-business-status.sql
```

### 3. 配置加密（jasypt）

敏感配置以 `ENC(密文)` 形式写入 `application.yml` / `application-dev.yml`：

| 文件 | 加密字段 |
| --- | --- |
| `application-dev.yml` | `datasource.username`、`datasource.password`、`redis.password`、`rabbitmq.username`、`rabbitmq.password` |
| `application.yml` | `super-mall.jwt.*-secret-key`、`super-mall.alioss.access-key-id`、`access-key-secret` |

加密参数（与 `jasypt-spring-boot` 默认一致）：

```
algorithm = PBEWITHHMACSHA512ANDAES_256
keyObtentionIterations = 1000
saltGenerator = RandomSaltGenerator
ivGenerator  = RandomIvGenerator
```

**新增 / 更换密文**（主密码自动从环境变量读取）：

```bash
# 用项目自带的 maven 插件生成密文
mvn -N jasypt:encrypt-value -Djasypt.plugin.value="要加密的明文"
# 输出：ENC(xxxxx)，把整串贴到 yml 对应字段即可
```

> 主密码由 `jasypt.encryptor.password: ${JASYPT_ENCRYPTOR_PASSWORD}` 提供，
> **切勿把主密码写进配置文件或提交到 Git**，否则加密就失去意义。

### 4. 商品图片（阿里云 OSS）

商品图不上仓库，统一放 OSS —— 项目换机器 / 换库后，图片 URL 依然可用。

- **配置**：`application.yml` 的 `super-mall.alioss`（AK/SK 为 jasypt 密文，`endpoint` / `bucket-name` 可用 `OSS_ENDPOINT` / `OSS_BUCKET` 覆盖）
- **上传接口**：`POST /api/merchant/file/upload`（商家端，`multipart/form-data`）
  - `file`：图片文件
  - `name`（可选）：指定 OSS 对象名，不传则用 UUID
  - 返回：可公网访问的 URL
- **存储**：`product.image` 存 OSS 完整 URL，例如
  `https://<bucket>.<endpoint>/supermall/products/beef-noodle.svg`

> 仓库里的 `superMall-takeout/src/main/resources/static/img/products/*.svg` 是**素材源文件**，
> 用于首次上传到 OSS；应用运行时并不读它们。

### 5. 平台 Logo（前端自备图片）

三端品牌位统一使用 `/logo.png`，图片自备：

```
frontend/public/logo.png          ← 当前使用
```

（旧三端各自的 `public/logo.png` 仅在回滚对照时用到）

图片未放置时不影响构建，前端会自动隐藏（不出现裂图）。

### 6. 启动

```bash
mvn clean package -DskipTests

# ① 外卖服务（8080）
java -jar superMall-takeout/target/superMall-takeout-0.0.1-SNAPSHOT.jar
# 或 IDE 直接运行 com.supermall.takeout.TakeoutApplication

# ② 电商服务（8081）—— 独立进程，可以只起其中一个
mysql --ssl-mode=DISABLED -uroot -p supermall < sql/mall.sql   # 首次需先建表
java -jar superMall-mall/target/superMall-mall-0.0.1-SNAPSHOT.jar
# 或 IDE 直接运行 com.supermall.mall.MallApplication
```

| 服务 | 端口 | 接口文档 | 启动成功标志 |
| --- | --- | --- | --- |
| 外卖 `superMall-takeout` | 8080 | `http://localhost:8080/doc.html` | `库存预热完成：商品 14 个` + `Started TakeoutApplication` |
| 电商 `superMall-mall` | 8081 | `http://localhost:8081/doc.html` | `Started MallApplication` |

两个服务共享 `sys_user` / `address` 两张表，其余全部隔离；
详见 [`docs/电商板块.md`](docs/电商板块.md)。

### 7. 默认账号（**账号是邮箱，密码统一 `Mall1234`**）

| 端 | 账号 | 登录接口 | Token 请求头 |
| --- | --- | --- | --- |
| 用户 | `user1@qq.com` / `user2@qq.com` | `POST /api/user/login` | `token` |
| 商家 | `merchant1@163.com`（老王牛肉面）/ `merchant2@163.com`（轻食沙拉铺）/ `merchant3@126.com`（惠民日用百货） | `POST /api/merchant/login` | `merchant-token` |
| 配送员 | `rider1@gmail.com` / `rider2@gmail.com` | `POST /api/delivery/login` | `delivery-token` |

登录/注册都要带**图形验证码**（先 `GET /api/auth/captcha`，答案存 Redis 且一次性）。
密码要求 **8-32 位且同时包含大写字母、小写字母、数字**；账号域名需在常用邮箱后缀白名单内。
规则可由 `GET /api/auth/policy` 获取。旧的 `user1` / `123456` 已由
`sql/migration-2026-09-18-email-account.sql` 迁移。

**电商板块（8081）**：买家直接复用上面的用户账号（`user1@qq.com` / `Mall1234`，请求头同样是 `token`）；
运营后台另有管理员：`admin@qq.com` / `Admin1234`，请求头 `mall-admin-token`。

> `merchant1` / `merchant2` 为外卖食品商家，`merchant3` 为百货商品商家，用于演示两个业态。

### 8. 前端（六端合一：`frontend`）

外卖三端（用户 / 商家 / 骑手）与电商两端（买家 / 运营后台）合并在**一个工程、一个开发端口**里，靠路由前缀区分：

| 身份 | 路由前缀 | 入口页 |
| --- | --- | --- |
| 外卖用户 | `/food/**` | `/food`（首页商品流） |
| 外卖商家 | `/merchant/**` | `/merchant`（订单看板） |
| 外卖骑手 | `/rider/**` | `/rider`（抢单池） |
| 电商买家 | `/shop/**` | `/shop`（商城首页） |
| 电商运营 | `/admin/**` | `/admin`（看板） |

`/` 是多端入口门户（`PortalView`），`/login`、`/register` 是统一登录/注册。

```bash
cd frontend && npm install && npm run dev   # http://localhost:5178
npm run build                               # 单个 dist/，由 Nginx :80 托管
npm run preview                             # 4178 预览生产构建
```

Vite 已配好双后端代理：`/api/mall` → `8081`（电商）、`/api` 与 `/img` → `8080`（外卖），不用处理跨域。

📖 工程说明见 [`frontend/README.md`](frontend/README.md)，接口手册见 `docs/外卖前端开发手册.md` / `docs/电商前端开发手册.md`。

> 旧的三个独立工程（`mall-web` / `mall-user-web` / `mall-merchant-web` / `mall-delivery-web`）已删除；
> 旧 e2e 脚本归档在 `scripts/legacy-frontend-e2e/`。

---

## 四、核心设计决策

| 能力 | 设计 |
| --- | --- |
| 三角色鉴权 | **用户 / 商家 / 配送员** 三套独立 JWT 拦截器与密钥，互不干扰 |
| 多商家数据隔离 | 商家业务表均带 `merchant_id`，Mapper 层强制 `where merchant_id = 当前登录商家` |
| 商品业态 | 两层结构：平台级业态 `business_type`（外卖 / 百货）+ 商家自定义分类 `category`；商品业态由分类**服务端派生** |
| 商家营业状态 | 商家自助「营业中 / 休息中」开关：**DB 落库为准 + Redis 缓存**，与平台停业状态 `merchant.status` 分离 |
| 购物车模型 | `cart`（用户 × 商家）+ `cart_item`，一个用户在每个商家下一辆购物车 |
| 按商家拆单 | 一次结算只针对一个商家，订单带 `merchant_id` |
| SKU | 独立 `sku` 表；有规格扣 SKU 库存，无规格扣商品库存 |
| 库存与超卖 | Redis + Lua 原子预扣作为第一道防线，DB 条件更新 `stock >= n` 作为第二道防线 |
| 配送体系 | 平台级配送员；`delivery_order` 与订单一对一，**Redis + Lua 抢单闸门** + DB 条件更新 |
| 支付抽象 | `PaymentService` 接口 + `MockPaymentService`，预留微信 / 支付宝扩展 |
| 订单超时 | 延迟队列自动关单 + 定时任务兜底 |
| 异步统计 | 订单完成发 MQ，消费者异步累加商品销量 |
| 状态管控 | 订单状态与配送状态分离；所有流转使用**条件更新**，天然幂等 |

---

## 五、数据库设计（`sql/supermall.sql`）

```
sys_user ─┬─ address
          ├─ cart ── cart_item
          └─ orders ─┬─ order_item
                     └─ delivery_order ── deliveryman

merchant ─┬─ category
          ├─ product ── sku
          └─ orders / cart / cart_item   (merchant_id 数据隔离)
```

| 表 | 作用 | 关键点 |
| --- | --- | --- |
| `sys_user` | 用户账号 | `username` 唯一 |
| `merchant` | 商家 | 一个商家一个账号；`status` 平台状态、`business_status` 商家自助营业状态 |
| `deliveryman` | 配送员 | 平台级，不固定隶属商家 |
| `category` | 分类 | `merchant_id`、`business_type`（所属业态） |
| `product` | 商品 | `merchant_id`、`category_id`、`business_type`（由分类派生）、库存 / 销量 |
| `sku` | 规格 | 有规格时库存挂在 SKU 上 |
| `address` | 收货地址 | `is_default` |
| `cart` / `cart_item` | 购物车 | 用户在每个商家下一辆购物车 |
| `orders` | 订单 | `status`（字符串枚举）、`pay_status`、**唯一索引 `(user_id, submit_token)` 幂等兜底** |
| `order_item` | 订单明细 | 下单时商品快照 |
| `delivery_order` | 配送单 | 与订单一对一，状态独立 |

### 订单状态机（与配送状态分离）

```
PENDING_PAYMENT ──► PAID ──► MERCHANT_ACCEPTED ──► WAITING_DELIVERY ──► DELIVERING ──► COMPLETED
       │              │
       └──────────────┴──────► CANCELLED

配送单： WAITING ──► ACCEPTED ──► DELIVERING ──► DELIVERED
```

**所有状态流转都使用条件更新**（`where status = 期望值`），天然幂等，是并发正确性的关键。

---

## 六、接口分组

按业务域组织，统一 `/api` 前缀：

| 前缀 | 鉴权 | 主要接口 |
| --- | --- | --- |
| `/api/public/**` | 无 | 商品流（按业态随机）、商家列表 / 详情 / 分类、商品详情（带缓存） |
| `/api/user/**` | `token` | 注册登录、资料、地址、购物车、下单、订单、支付 |
| `/api/merchant/**` | `merchant-token` | 登录、店铺信息、**营业状态开关**、分类、商品、SKU、订单（接单 / 备餐 / 拒单）、图片上传到 OSS |
| `/api/delivery/**` | `delivery-token` | 登录、资料、抢单池、抢单、取餐、送达、统计 |

### 一次完整下单链路（可直接用 Knife4j 串起来）

```text
1. 用户登录                     POST /api/user/login
2. 浏览商家 / 商品               GET  /api/public/merchants/{id}/products
3. 加入购物车                    POST /api/user/cart
4. 获取下单令牌                  GET  /api/user/orders/token
5. 提交订单                      POST /api/user/orders/submit
6. 获取支付令牌                  GET  /api/user/payment/token?orderNumber=xxx
7. 支付（Mock）                  POST /api/user/payment/pay
8. 商家登录 → 接单               PUT  /api/merchant/orders/accept/{id}
9. 商家备餐完成 → 生成配送单      PUT  /api/merchant/orders/ready/{id}
10. 配送员登录 → 抢单            PUT  /api/delivery/orders/grab/{orderId}
11. 取餐                         PUT  /api/delivery/orders/pickup/{orderId}
12. 确认送达                     PUT  /api/delivery/orders/delivered/{orderId}
```

> 开发调试时若不想走支付全流程，可直接调用
> `PUT /api/merchant/orders/simulate-pay/{orderNumber}` 模拟支付成功。

---

## 七、关键技术点（对应大纲的学习目标）

| 目标 | 落点 |
| --- | --- |
| 统一返回 / 全局异常 / 参数校验 | `Result`、`GlobalExceptionHandler`、`@Valid` + `jakarta.validation` |
| 多商家数据隔离 | `JwtTokenMerchantInterceptor` 注入 `merchantId` → Mapper `where merchant_id` |
| 商品业态派生 | `ProductServiceImpl` 保存/更新时用 `category.business_type` 回填 `product.business_type`，忽略前端传值 |
| 营业状态缓存 | `MerchantServiceImpl`：读 Redis → 未命中回源 DB 并回写；写库后写穿缓存（TTL 30 分钟自愈） |
| 缓存三大问题 | `ProductServiceImpl.getDetail`：空值缓存防穿透、`setnx` 互斥锁 + 双重检查防击穿、随机 TTL 防雪崩 |
| 缓存三大问题（电商） | `MallProductServiceImpl.detailCached` + `MallRedisCacheClient`（P8 提前交付）：同款三防，key 换 `mall:product:detail:` / `mall:cache:lock:`；保存/上下架/删除商品删缓存，后台详情直查 DB |
| 防超卖 | `StockServiceImpl`：Redis + Lua 原子预扣；`OrderServiceImpl.deductDbStock`：DB 条件更新二次保证 |
| 幂等 | 下单 / 支付：Redis 令牌（Lua 原子消费）+ DB 唯一索引双保险；状态流转全部条件更新 |
| 抢单并发 | `DeliveryServiceImpl.grab`：Redis + Lua 闸门 + DB `acceptIfWaiting` 条件更新 |
| 防重复提交 | `@NoRepeatSubmit` + `NoRepeatSubmitAspect`（SETNX + Lua 释放） |
| 接口限流 | `@RateLimit` + `rate_limit.lua`（按 IP + 方法，固定窗口计数） |
| MQ 延迟关单 | `RabbitMQConfig`（TTL + 死信 + 重试队列）+ `OrderTimeoutConsumer` + `OrderTask` 兜底 |
| MQ 异步处理 | 支付成功 `PaySuccessConsumer`、订单完成 `OrderStatConsumer` |
| 事务 | 下单、取消、接单备餐等 `@Transactional`，失败回滚 DB 并手动回补 Redis 库存 |
| 支付可扩展 | `PaymentService` / `MockPaymentService` |
| 全链路日志 | `TraceIdInterceptor` + MDC + Logback |

---

## 八、测试与压测

### 单元测试（Mockito，不依赖中间件）

```bash
mvn -pl superMall-takeout -am test \
  -Dtest='BusinessTypeTest,MerchantServiceImplTest,ProductServiceImplTest,CategoryServiceImplTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
```

### 全量测试（含集成 + 并发，需本地 MySQL / Redis / RabbitMQ + 主密码环境变量）

```bash
mvn test
```

包含：

- **单元测试**：业态枚举、营业状态缓存逻辑、商品业态派生、分类判重；
- **集成测试**：`MerchantBusinessStatusIntegrationTest`（DB + Redis 完整链路）；
- **并发测试**：100 线程抢 10 件库存、100 线程打 DB 条件扣减、50 线程消费同一幂等令牌、50 骑手抢同一单。

> 5 个 `@SpringBootTest` 使用 `webEnvironment = RANDOM_PORT`：项目含 WebSocket `ServerEndpointExporter`，
> 默认 MOCK 环境缺少 Servlet 容器会启动失败。

### HTTP 层压测脚本（`scripts/loadtest/`，Node 18+）

```bash
# 首页商品流压测（无限流接口）
node scripts/loadtest/feed-load.mjs --concurrency 30 --requests 600

# 验证接口限流（/api/user/orders/token 限 30/60s）
REDIS_PASSWORD=<redis密码> node scripts/loadtest/rate-limit.mjs --concurrency 100

# 端到端抢购：15 买家抢 10 件，验证不超卖
REDIS_PASSWORD=<redis密码> node scripts/loadtest/order-race.mjs --users 15 --stock 10
```

| 脚本 | 用途 | 实测 |
| --- | --- | --- |
| `feed-load.mjs` | 吞吐 / 延迟分布 | 152 QPS，p50 150ms |
| `rate-limit.mjs` | 限流是否生效 | 100 并发 → 放行 30、限流 70 |
| `order-race.mjs` | 端到端不超卖 | 15 买家抢 10 件 → 10 成功、5 库存不足 |

> **重要**：`/api/user/orders/submit` 有 `@RateLimit(15/60s)`，且按 `getRemoteAddr()` 取 IP、
> 不认 `X-Forwarded-For`。因此**单机 HTTP 100 并发下单会被限流挡住**（防刷设计生效）。
> 「并发正确性」验证请用 JUnit 并发测试；HTTP 脚本用于吞吐压测与限流验证。
> 详见 [`scripts/loadtest/README.md`](scripts/loadtest/README.md)。

---

## 九、与大纲开发阶段的对应关系

| 大纲阶段 | 本项目落点 |
| --- | --- |
| 一、基础 CRUD | `Category` / `Product` / `Sku` 商家端完整 CRUD + 分页 + 校验 + 业态分类 |
| 二、购物车 | `cart` + `cart_item`，加购 / 改数量 / 删除 / 按商家分组查询 |
| 三、订单 | `OrderServiceImpl.submitOrder`：拆单、生成明细、扣库存、清购物车 |
| 四、支付 | `MockPaymentService` + 支付幂等令牌 + `paySuccess` 条件更新 |
| 五、商家流程 | 接单 → 备餐完成 → 生成配送单 |
| 六、配送 | 抢单池 → Redis + Lua 抢单 → 取餐 → 送达 |
| 七、RabbitMQ | 延迟关单、重试、支付成功异步、订单完成异步统计 |

> 大纲第十五节「暂不实现」的部分（平台管理端、RBAC、优惠券、真实支付、结算佣金等）本项目均未引入。

---

## 十、注意事项

1. **Redis / RabbitMQ 未启动**：应用仍可启动（库存预热失败只告警），但下单会提示「库存未预热」，
   抢单与幂等能力不可用。请务必保证中间件可用。
2. **前端**：三端已合并为 `frontend`（单工程单端口 5173），改造说明见 [`frontend/README.md`](frontend/README.md)；
   旧三端工程保留作回滚对照，端口 5174 / 5175。
3. **平台 Logo**：图片自备，放入各前端 `public/logo.png`，未放置时自动隐藏且不影响构建。
4. **主密码**：所有敏感配置已用 jasypt 加密，启动**必须**提供环境变量 `JASYPT_ENCRYPTOR_PASSWORD`，
   否则会因 `${JASYPT_ENCRYPTOR_PASSWORD}` 无法解析而启动失败。主密码本身不要提交到 Git。
   生产环境建议把主密码放到 CI/CD 密钥管理或启动脚本里，而非用户环境变量。
5. **单价计算**：下单金额一律由服务端根据购物车重新计算，不信任前端传值。
6. **数据库迁移**：本项目不使用自动迁移工具，结构变更以 `sql/migration-*.sql` 形式提供，升级已有库时手动执行。
7. **部署**：📖 **完整部署指南见 [`deploy/README.md`](deploy/README.md)**
   （前置条件 / 一条命令起全套 / 环境变量清单 / 运维命令 / 升级含 DB 迁移 / 换机器迁移 / 备份恢复 / 排查手册 / 生产化清单）。

   ```bash
   cp deploy/.env.example .env        # 填 JASYPT_ENCRYPTOR_PASSWORD
   docker compose up -d --build       # 中间件 + 两个后端 + 前端，全在容器里构建
   # → 前端 http://localhost ｜ 外卖文档 :8080/doc.html ｜ 电商文档 :8081/doc.html
   ```
   宿主机已有 MySQL/Redis/RabbitMQ 时用 `.env` 的 `*_HOST_PORT` 避开端口冲突；演示图在 OSS，不需要随镜像分发。

   本机当前用的是**非 Docker** 方式（Nginx `D:\nginx\nginx-1.26.3` :80 托管 `frontend/dist` + 反代两个后端），
   模板 [`deploy/nginx.local.conf`](deploy/nginx.local.conf)，步骤见指南第 10 节。

---

