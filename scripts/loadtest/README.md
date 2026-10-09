# HTTP 层压测脚本

零依赖（Node 18+ 内置 `fetch`），用于对**运行中的后端**（默认 `http://localhost:8080`）做 HTTP 压力/并发验证。
正确性验证（100 并发抢库存、幂等、抢单）请用 `superMall-takeout/src/test` 下的 JUnit 并发测试。

## 前置条件

- 后端已启动（`mvn spring-boot:run` 或 `java -jar superMall-takeout/target/*.jar`）
- MySQL / Redis / RabbitMQ 已启动
- Node 18+（本机 v24 已满足）

## 脚本

### 1. `feed-load.mjs` — 首页商品流压测（无限流）

```bash
node scripts/loadtest/feed-load.mjs --concurrency 50 --requests 2000
node scripts/loadtest/feed-load.mjs --type 2 --concurrency 20 --requests 500
```

输出：总请求、成功/失败、QPS、p50/p90/p99/max 延迟、失败原因分布。

### 2. `rate-limit.mjs` — 验证接口限流

目标 `GET /api/user/orders/token`（`@RateLimit(30/60s)`，按 IP + 方法）。
一次性并发 100 个请求，应只有 ≤30 个放行，其余返回「请求过于频繁」。

```bash
node scripts/loadtest/rate-limit.mjs --concurrency 100
# 想让结果可复现（自动重置限流窗口）：
REDIS_PASSWORD=<redis密码> node scripts/loadtest/rate-limit.mjs --concurrency 100
```

> 限流实现用的是 `request.getRemoteAddr()`，**不认 `X-Forwarded-For`**，所以同一台机器发出的请求共用一个限流桶。

### 3. `order-race.mjs` — 端到端抢购（不超卖验证）

自动完成：商家登录 → 设置库存 → 造 N 个买家（注册/地址/加购/令牌）→ 同时下单 → 校验不超卖 → 取消订单回补库存 → 恢复原库存。
最后打印**压测买家账号的清理 SQL**（没有删除用户接口，需手动执行）。

```bash
node scripts/loadtest/order-race.mjs --users 15 --stock 10
node scripts/loadtest/order-race.mjs --product 3 --users 15 --stock 10
```

## ⚠️ 为什么默认是 15 个买家，而不是 100

`/api/user/orders/submit` 上有 `@RateLimit(limit = 15, window = 60)`，且限流维度是 **IP + 方法**。
单机发起 100 个并发下单，只有 15 个能过限流，其余直接返回「请求过于频繁」——**这是防刷设计生效**。

因此：

| 目标 | 用什么 |
| --- | --- |
| 验证 HTTP 全链路 + 限流协作 | `order-race.mjs --users 15` |
| 验证"100 并发抢 10 件库存不超卖" | JUnit：`StockDeductConcurrencyTest` / `DbStockDeductConcurrencyTest` |
| 验证限流本身 | `rate-limit.mjs --concurrency 100` |

想真在 HTTP 层做 100 并发下单一对一压测，需要：调大 `@RateLimit` 参数、或从多个 IP 发起、或在压测环境关掉限流切面。

## 环境变量

| 变量 | 用途 |
| --- | --- |
| `REDIS_PASSWORD` | 有值时，脚本用 `redis-cli` 重置限流窗口（结果可复现） |
| `REDIS_HOST` / `REDIS_PORT` | redis-cli 连接地址，默认 `127.0.0.1:6379` |

## 提示

- 脚本会真实写数据（注册用户、建地址、下单）。`order-race.mjs` 会自动取消订单并恢复库存，但**用户账号需按输出 SQL 手动清理**。
- 建议只在开发库上跑。
