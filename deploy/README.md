# superMall 部署指南（Docker Compose · 单机）

> **一句话**：装好 Docker → `cp deploy/.env.example .env` 填主密码 → `docker compose up -d --build` → 打开 <http://localhost>。
> 宿主机**不需要** JDK / Maven / Node —— 前端与后端都在容器里构建。
>
> 演示图在 **OSS**（DB 里存绝对 URL），所以部署/迁移只需要「能连数据库 + 能出网访问 OSS」，镜像里不带图片。

**目录**

| # | 章节 | 什么时候看 |
| --- | --- | --- |
| [0](#0-前置条件) | 前置条件（机器要求） | 部署前 |
| [1](#1-一条命令起全套) | 一条命令起全套 | 首次部署 |
| [2](#2-环境变量清单env) | 环境变量清单 | 首次部署 / 改密码 |
| [3](#3-架构与端口) | 架构与端口 | 想搞清楚谁连谁 |
| [4](#4-常用运维命令) | 常用运维命令 | 日常 |
| [5](#5-升级到新版本) | 升级到新版本（含 DB 变更） | 有新代码/新表结构时 |
| [6](#6-迁移到新机器) | 迁移到新机器 | 换机器 / 上云 |
| [7](#7-备份与恢复) | 备份与恢复 | 动数据前 |
| [8](#8-排查手册) | 排查手册 | 起不来 |
| [9](#9-生产化清单上线前) | 生产化清单 | 真上线前 |
| [10](#10-不用-docker本机-nginxwindows) | 不用 Docker（本机 Nginx） | Windows 本机演示 |

---

## 0. 前置条件

| 项 | 要求 | 说明 |
| --- | --- | --- |
| Docker | **Docker Desktop 4.x / Engine 20.10+**，且是 **Compose v2**（命令是 `docker compose`，不是 `docker-compose`） | compose 文件用了 `depends_on: condition: service_healthy`（v2 语法），没有 `version:` 字段 |
| 内存 | 可用 **≥ 4GB**（建议 8GB） | 两个 JVM（各 `-Xmx512m`）+ MySQL + RabbitMQ + Redis + 构建期 Node/Maven |
| 磁盘 | **≥ 6GB** | 基础镜像约 1.5GB + Maven/npm 依赖缓存 + 两个 jar（66/68MB）+ 数据卷 |
| 网络 | 能拉镜像、能下 Maven/npm 依赖、**能访问 OSS** | 图在 OSS，访问不了 → 页面图是灰占位（功能不受影响） |
| 端口 | `80 / 8080 / 8081 / 15672 / 3306 / 6379 / 5672` 空闲 | 被占用不用改 compose，改 `.env` 的 `*_HOST_PORT` 即可（见 §2） |

**首次构建耗时 5~15 分钟**（视网速）：拉 7 个基础镜像 + Maven 依赖 + npm 依赖；之后重建很快（有层缓存）。

---

## 1. 一条命令起全套

```bash
# ① 环境变量（只需填 jasypt 主密码，其余用默认值即可）
cp deploy/.env.example .env

# ② 构建 + 启动（后台运行）
docker compose up -d --build

# ③ 看状态与日志
docker compose ps
docker compose logs -f mall        # 或 takeout / nginx / mysql
```

**成功标志**

| 检查 | 期望 |
| --- | --- |
| `docker compose ps` | 6 个服务都 `Up`（`mysql/redis/rabbitmq` 带 `healthy`；`mall`/`takeout` 会在中间件 healthy 之后启动） |
| `curl -s -o /dev/null -w '%{http_code}' http://localhost/` | `200` |
| `curl -s http://localhost/api/mall/catalog/categories \| head -c 60` | `{"code":1,...` （证明 nginx → 电商容器 通） |
| `docker compose exec -T mysql mysql -uroot -pMing1234 -N -B -e "select count(*) from mall_product" supermall` | `69`（种子数据：3 个基础商品 + 66 个演示商品） |

**访问入口**（端口都能在 `.env` 改）

| 入口 | 地址 |
| --- | --- |
| 前端（六端合一） | <http://localhost> |
| 外卖接口文档 Knife4j | <http://localhost:8080/doc.html> |
| 电商接口文档 Knife4j | <http://localhost:8081/doc.html> |
| RabbitMQ 控制台 | <http://localhost:15672>（`guest / guest`） |

**演示账号**（登录要图形验证码）

| 端 | 账号 | 密码 | 请求头 |
| --- | --- | --- | --- |
| 外卖用户 / 电商买家 | `user1@qq.com`、`user2@qq.com` | `Mall1234` | `token` |
| 外卖商家 | `merchant1@163.com`、`merchant2@163.com`、`merchant3@126.com` | `Mall1234` | `merchant-token` |
| 外卖骑手 | `rider1@gmail.com`、`rider2@gmail.com` | `Mall1234` | `delivery-token` |
| 外卖平台运营 | `platform@163.com` | `Admin1234` | `platform-token` |
| 电商运营后台 | `admin@qq.com` | `Admin1234` | `mall-admin-token` |

---

## 2. 环境变量清单（`.env`）

`.env` 放在**仓库根目录**（已被 `.gitignore` 忽略，不会进仓库）。模板：`deploy/.env.example`。

| 变量 | 默认值 | 必填 | 说明 |
| --- | --- | --- | --- |
| `JASYPT_ENCRYPTOR_PASSWORD` | 无 | **是** | jasypt 主密码。**错了容器起不来**（日志里是 `Unable to decrypt` / 一堆 `ENC(...)`）。没填时 compose 直接拒绝启动 |
| `MYSQL_ROOT_PASSWORD` | `Ming1234` | 否 | MySQL root 密码，**必须与 yml 里 `ENC(...)` 的解密值一致** |
| `REDIS_PASSWORD` | `mehado` | 否 | Redis 密码，同上 |
| `MYSQL_HOST_PORT` | `3306` | 否 | 宿主端口。本机已装 MySQL 就改（如 `13306`） |
| `REDIS_HOST_PORT` | `6379` | 否 | 同上 |
| `RABBITMQ_HOST_PORT` | `5672` | 否 | 同上 |
| `RABBITMQ_CONSOLE_PORT` | `15672` | 否 | 控制台宿主端口 |
| `TAKEOUT_HOST_PORT` | `8080` | 否 | 外卖后端宿主端口 |
| `MALL_HOST_PORT` | `8081` | 否 | 电商后端宿主端口 |
| `NGINX_HOST_PORT` | `80` | 否 | 前端入口宿主端口 |

> **改宿主端口不影响功能**：容器之间走服务名 + 容器内端口（`mysql:3306` / `redis:6379` / `rabbitmq:5672`）。

### 2.1 哪些配置能用环境变量覆盖，哪些不能

| 类别 | 能否用环境变量覆盖 | 例子 |
| --- | --- | --- |
| 中间件地址 / 库名 | ✅ 能（compose 已内置） | `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DATABASE`、`REDIS_HOST`、`REDIS_PORT`、`RABBITMQ_HOST`、`RABBITMQ_PORT` |
| OSS endpoint / bucket | ✅ 能 | `OSS_ENDPOINT`、`OSS_BUCKET` |
| **账号 / 密码 / 密钥 / OSS AK·SK / 6 套 JWT 密钥** | ❌ **不能**（yml 里是 jasypt 密文） | 改这些要**重新加密**（见下） |

### 2.2 `ENC(...)` 一共 18 处，换主密码就得全部重加密

别人拿到这个仓库想部署，只有两条路：**① 你告诉他主密码**；**② 他用自己的主密码把这 18 处重新加密**。

```bash
# 生成密文（要先有主密码：set JASYPT_ENCRYPTOR_PASSWORD=xxx，然后重启终端）
mvn -N jasypt:encrypt-value -Djasypt.plugin.value="明文"
```

要替换的位置：

| 文件 | 条目 |
| --- | --- |
| `superMall-takeout/src/main/resources/application-dev.yml` | MySQL 用户名 / 密码、Redis 密码、RabbitMQ 用户名 / 密码（5） |
| `superMall-mall/src/main/resources/application-dev.yml` | MySQL 用户名 / 密码、Redis 密码（3） |
| `superMall-takeout/src/main/resources/application.yml` | JWT：user / merchant / delivery / platform 密钥（4）、OSS AK / SK（2） |
| `superMall-mall/src/main/resources/application.yml` | JWT：user（与外卖同一把）/ mall-admin 密钥（2）、OSS AK / SK（2） |

---

## 3. 架构与端口

| 服务 | 容器名 | 宿主端口（`.env`） | 镜像 / 构建 | 说明 |
| --- | --- | --- | --- | --- |
| MySQL | `supermall-mysql` | 3306 → `MYSQL_HOST_PORT` | `mysql:8.0` | 首次自动执行 `sql/*.sql`（见下） |
| Redis | `supermall-redis` | 6379 → `REDIS_HOST_PORT` | `redis:5.0-alpine` | 刻意用 5.0：与开发环境一致（不用 6.2+ 才有的命令） |
| RabbitMQ | `supermall-rabbitmq` | 5672 / 15672 | `rabbitmq:3-management-alpine` | 外卖延迟关单用；未启动不影响主流程（有定时任务兜底） |
| 外卖后端 | `supermall-takeout` | 8080 → `TAKEOUT_HOST_PORT` | `deploy/Dockerfile.backend` + `MODULE=superMall-takeout` | `-Xmx512m` |
| 电商后端 | `supermall-mall` | 8081 → `MALL_HOST_PORT` | `deploy/Dockerfile.backend` + `MODULE=superMall-mall` | 同上 |
| 前端 + Nginx | `supermall-nginx` | 80 → `NGINX_HOST_PORT` | `deploy/Dockerfile.frontend`（node 构建 → nginx） | 静态资源 + `/api` 反代 |

```
浏览器 :80 ──► nginx ──┬── /                → 前端 dist（六端合一）
                       ├── /api/mall/**     → mall:8081（电商后端）
                       └── /api/** /img/**  → takeout:8080（外卖后端）
```

**MySQL 首次初始化**（数据卷为空时才执行，按文件名顺序）：

| 文件 | 内容 | 幂等？ |
| --- | --- | --- |
| `01-supermall.sql` | 外卖：建库 + 建表 + 种子（3 商家 / 8 分类 / 14 商品 / 2 骑手 / 2 用户） | ❌ **删表重建**，只在首次跑 |
| `02-mall.sql` | 电商：建表（`mall_` 前缀）+ 种子 + 索引（内部用 `IF NOT EXISTS` / 存储过程） | ✅ 可重跑 |
| `03-seed-mall-demo.sql` | 电商演示数据：69 个带图商品 / 30 分类 / 12 推荐位（图片是 **OSS 绝对 URL**） | ✅ 可重跑（只动种子 id 区间） |

> 不想要演示数据？删掉 compose 里 `03-seed-mall-demo.sql` 那一行挂载，再 `docker compose down -v && up -d`。

---

## 4. 常用运维命令

```bash
# 状态 / 日志
docker compose ps
docker compose logs -f                 # 全部
docker compose logs -f --tail=200 mall # 单个服务

# 启停
docker compose stop                    # 停（保留容器与数据）
docker compose start                   # 再起
docker compose restart mall takeout    # 只重启后端
docker compose down                    # 删容器（保留数据卷与镜像）
docker compose down -v                 # 连数据卷一起删 → 下次 up 重新建表 + 种子
docker compose down -v --rmi local     # 再把本地构建的镜像也删掉

# 只重建某个服务
docker compose up -d --build mall      # 只重建电商后端
docker compose up -d --build nginx     # 只重建前端

# 进容器 / 查环境
docker compose exec takeout sh
docker compose exec nginx nginx -T     # 看 nginx 实际生效的配置
docker compose exec mysql bash
docker compose exec -T mysql mysql -uroot -pMing1234 supermall -e "select count(*) from mall_order"
docker compose exec redis redis-cli -a mehado ping
```

---

## 5. 升级到新版本

### 5.1 只改了代码（表结构没变）

```bash
git pull
docker compose up -d --build            # 重建后端 + 前端（有层缓存，通常 2~5 分钟）
docker compose ps
```

只改了前端：`docker compose up -d --build nginx`；只改了后端：`docker compose up -d --build mall`（或 `takeout`）。

### 5.2 有表结构变更（重要）

**已有的库不会被自动升级** —— 初始化 SQL 只在数据卷为空时跑。表结构变更必须手动执行迁移脚本：

```bash
# 逐个执行（都是幂等的，重复执行安全）
docker compose exec -T mysql mysql -uroot -pMing1234 supermall < sql/migration-2026-09-17-business-type.sql
docker compose exec -T mysql mysql -uroot -pMing1234 supermall < sql/migration-2026-09-17-merchant-business-status.sql
docker compose exec -T mysql mysql -uroot -pMing1234 supermall < sql/migration-2026-09-18-email-account.sql
docker compose exec -T mysql mysql -uroot -pMing1234 supermall < sql/migration-2026-09-19-mall-p3.sql
docker compose exec -T mysql mysql -uroot -pMing1234 supermall < sql/migration-2026-09-20-mall-p4.sql
docker compose exec -T mysql mysql -uroot -pMing1234 supermall < sql/migration-2026-09-21-mall-p5.sql
docker compose exec -T mysql mysql -uroot -pMing1234 supermall < sql/migration-2026-09-21-perf-indexes.sql
docker compose exec -T mysql mysql -uroot -pMing1234 supermall < sql/migration-2026-09-22-platform-onboarding.sql

docker compose restart takeout mall     # 让后端重新读表 / 预热库存缓存
```

| 脚本 | 干什么 |
| --- | --- |
| `migration-2026-09-17-business-type.sql` | 外卖：商品业态（外卖食品 / 百货商品）+ 索引 + 百货演示数据 |
| `migration-2026-09-17-merchant-business-status.sql` | 外卖：商家自助营业状态列 |
| `migration-2026-09-18-email-account.sql` | 账号改邮箱 + 密码统一（含历史账号迁移） |
| `migration-2026-09-19-mall-p3.sql` | 电商：购物车明细表 + `address.area_code` |
| `migration-2026-09-20-mall-p4.sql` | 电商：订单 / 订单明细 / 物流单 / 退款表 |
| `migration-2026-09-21-mall-p5.sql` | 电商：评价 / 评价图 + 商品评分聚合列 |
| `migration-2026-09-21-perf-indexes.sql` | 性能索引 4 个（订单 / 配送单 / 统计） |
| `migration-2026-09-22-platform-onboarding.sql` | 外卖：平台运营表 + 商家入驻审核列 + 平台种子账号 |

> ⚠️ **千万不要在已有数据的库上重跑 `sql/supermall.sql`** —— 它是 `DROP TABLE` + 重建的**全新安装**脚本，会把外卖数据清空。
> `sql/mall.sql` 与 `sql/seed-mall-demo.sql` 可以安全重跑（幂等）。

### 5.3 回滚

```bash
git log --oneline -5                    # 找到上一个提交
git checkout <旧提交>
docker compose up -d --build            # 用旧代码重建
# DB 回滚：从 §7 的备份恢复（表结构变更一般无法自动回退）
```

---

## 6. 迁移到新机器

### 情况 A：只要演示环境（推荐，最省事）

```bash
git clone https://gitee.com/mehado/supermall-demo001.git && cd supermall-demo001
cp deploy/.env.example .env             # 填 JASYPT_ENCRYPTOR_PASSWORD
docker compose up -d --build            # 自动建表 + 种子数据（图片是 OSS 绝对 URL）
```
图片**不用搬** —— 它们在 OSS，只要新机器能访问 OSS 就有图。

### 情况 B：要搬**现有业务数据**

```bash
# 旧机器：导出（结构 + 数据）
docker compose exec -T mysql mysqldump -uroot -pMing1234 \
  --databases supermall --single-transaction --default-character-set=utf8mb4 \
  > supermall-dump.sql

# 新机器：先让 compose 把库建好，再导入覆盖
docker compose up -d mysql
docker compose exec -T mysql mysql -uroot -pMing1234 supermall < supermall-dump.sql
docker compose up -d                     # 起其余服务
docker compose restart takeout mall      # 重新预热库存/缓存
```

> 导入会覆盖 initdb 的种子数据（正常，因为你要的是"现有数据"）。
> 若两边的 jasypt 主密码不同，记得按 §2.2 重新加密后再起后端。

### 换 OSS bucket / 换 OSS 账号

```bash
# 1) 改 application.yml 的 bucket-name（+ AK/SK 重新加密，见 §2.2）
# 2) 把图重传一遍，并改好 DB 与种子 SQL 里的地址
node scripts/migrate-seed-images-to-oss.mjs            # 先 dry-run 看要做什么
node scripts/migrate-seed-images-to-oss.mjs --apply
```

---

## 7. 备份与恢复

```bash
# 备份（导出到宿主机当前目录）
docker compose exec -T mysql mysqldump -uroot -pMing1234 \
  --databases supermall --single-transaction --default-character-set=utf8mb4 \
  > backup-$(date +%Y%m%d-%H%M).sql

# 恢复
docker compose exec -T mysql mysql -uroot -pMing1234 supermall < backup-20260921-1930.sql

# 只想备份某几张表
docker compose exec -T mysql mysqldump -uroot -pMing1234 supermall mall_order mall_order_item > orders.sql
```

- **Redis 不用备份**：里面只有库存预扣 / 幂等令牌 / 验证码这类临时数据；丢了会导致「库存未预热」，重启后端会自动重新预热。
- **OSS 上的演示图**：建议单独保留一份本地副本（仓库外的 `frontend/public/seed/` 就是这份源文件）。

---

## 8. 排查手册

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `port is already allocated` / `bind: address already in use` | 宿主端口被占（本机装了 MySQL/Redis/RabbitMQ 最常见） | 改 `.env` 的 `*_HOST_PORT`（见 §2），或停掉本机服务 |
| 容器起来就退出，日志 `Unable to decrypt`、`ENC(...)` | `JASYPT_ENCRYPTOR_PASSWORD` 没填 / 填错 | 检查 `.env`（主密码错了所有密文都解不开） |
| `docker compose up` 直接报 `required variable JASYPT_ENCRYPTOR_PASSWORD is missing` | `.env` 不存在 | `cp deploy/.env.example .env` 并填主密码 |
| 后端日志 `Communications link failure` / 启动失败 | MySQL 还没就绪 | 正常会等健康检查；持续失败看 `docker compose logs mysql`（初始化 SQL 报错会在这里） |
| `docker compose ps` 里 mysql 一直 `starting` | 首次初始化 3 个 SQL 需要时间；或数据卷里有**旧的、不兼容**的数据 | 等 1~2 分钟；实在不行 `down -v` 重来（**会清数据**） |
| 页面打开但**图全是灰** | 容器访问不到 OSS（出网 / DNS / bucket 权限） | `curl -sI https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/seed/c-001.jpg \| head -1` 应为 `200`；容器内也可 `docker compose exec nginx wget -S -O /dev/null <url>` |
| 直接访问 `http://localhost/shop/xxx` 刷新 404 | SPA history 回退没生效 | 确认用的是仓库里的 `deploy/nginx.conf`（`try_files ... /index.html`） |
| 前端 build 阶段失败（`JavaScript heap out of memory` / 网络超时） | 构建机内存不足 / npm 源慢 | 给 Docker 更多内存；或改用宿主机构建 + 挂载 `frontend/dist`（§1 末尾的备选写法） |
| 后端 build 阶段 Maven 下载卡住 | 网络慢 / 没配镜像 | 重试 `docker compose build mall`；或给 Maven 配国内镜像（改 `deploy/Dockerfile.backend` 里加 `settings.xml`） |
| 登录提示「验证码已过期」 | Redis 里验证码 120s 到期（正常，一次性） | 重新打开登录页 |
| 想看容器网络 / 环境 | — | `docker compose exec takeout env`、`docker compose exec nginx nginx -T` |
| 日志里时间是 UTC / 差 8 小时 | 容器时区 | 后端镜像已设 `TZ=Asia/Shanghai`；其它容器按需加 `TZ` 环境变量 |
| 想彻底重来 | — | `docker compose down -v --rmi local`（**删数据卷 + 本地镜像**）再 `up -d --build` |

**排查三连**：`docker compose ps`（谁不在）→ `docker compose logs --tail=200 <服务>`（为什么）→ `docker compose exec <服务> env`（配置对不对）。

---

## 9. 生产化清单（上线前）

| # | 项 | 现状（演示） | 上线要做的 |
| --- | --- | --- | --- |
| 1 | 主密码与数据库密码 | 默认 `Ming1234` / `mehado` | 换强密码 → `mvn -N jasypt:encrypt-value` 重新加密 §2.2 的 18 处 |
| 2 | RabbitMQ 账号 | `guest / guest` | 建独立用户 + vhost 并加密写进 yml |
| 3 | 接口文档 | Knife4j `/doc.html` 开启 | 生产关掉（`knife4j.enable=false` / `springdoc.api-docs.enabled=false`） |
| 4 | 演示图 | 第三方照片（OSS，仅自用） | 换成自有素材（覆盖 OSS 同名对象即可，DB 不用改） |
| 5 | OSS 权限 | bucket 公共读 | 配 CDN + 防盗链 / 私有 bucket + 签名 URL（需改前端取图逻辑） |
| 6 | HTTPS | 只有 HTTP :80 | Nginx 前置 TLS（证书 + 301 跳转），或挂云负载均衡 |
| 7 | 资源限制 | 未限制 | 给 compose 服务加 `deploy.resources.limits` / `mem_limit`，防单个容器拖垮机器 |
| 8 | 备份 | 手动 | 定时任务 + 异地存放；OSS 图片也留副本 |
| 9 | 日志 | 容器 stdout（默认 json-file） | 配 `log-opts` 限制大小，或接 ELK / 云日志 |
| 10 | 支付 / MQ 可靠性 | 支付是 Mock；外卖 MQ 挂了有定时任务兜底 | 接真实支付（支付宝沙箱/正式）、MQ 加持久化与重试告警 |

---

## 10. 不用 Docker：本机 Nginx（Windows）

本机已按这种方式跑通（nginx 在 `D:\nginx\nginx-1.26.3`，直接覆盖 `conf/nginx.conf`，单文件无 include）。
模板：[`deploy/nginx.local.conf`](nginx.local.conf)（记得把 `root` 改成你的 `frontend/dist` 绝对路径）。

```bash
# 1) 前端产物
cd frontend && npm install && npm run build

# 2) 覆盖配置（模板里 root 改成你的实际路径）
copy deploy\nginx.local.conf D:\nginx\nginx-1.26.3\conf\nginx.conf

# 3) 校验 + 启动
cd /d D:\nginx\nginx-1.26.3
nginx -t && start nginx
# 改完配置：nginx -s reload ｜ 停止：nginx -s quit
```

| 路径 | 转发到 |
| --- | --- |
| `/api/mall/**` | 电商后端 `127.0.0.1:8081` |
| `/api/**` / `/img/**` | 外卖后端 `127.0.0.1:8080` |
| `/` | `frontend/dist`（history 模式回退 `index.html`） |

前端请求用相对路径（`VITE_API_BASE` 留空），所以同一份构建产物既能跑 dev server，也能被 Nginx / Docker 反代，不需要为部署单独打包。

---

## 附：本指南的验证状态

| 项 | 状态 |
| --- | --- |
| `docker compose config`（YAML / 变量插值 / 端口映射 / 三个 SQL 挂载 / 前端 build） | ✅ 已通过 |
| `docker compose up -d --build` 全流程 | ⚠️ 未在本机跑完（首次构建到 Maven 依赖阶段即停止，已清理容器/卷/镜像/构建缓存）。首次启动请按 §1 的「成功标志」逐条自检 |
| 非 Docker 的 Nginx 方式（§10） | ✅ 本机已在用并跑通 |
