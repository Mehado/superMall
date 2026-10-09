# superMall 一体化前端（电商 + 外卖，六端合一）

一个 Vue 3 单页工程，按 `docs/` 下的两份手册 + 设计 brief 实现**纯前端**页面：

- [`docs/电商前端开发手册.md`](../docs/电商前端开发手册.md) → 电商商城买家端 + 电商运营后台
- [`docs/外卖前端开发手册.md`](../docs/外卖前端开发手册.md) → 外卖用户端 + 外卖商家端 + 外卖骑手端
- [`docs/前端风格提示.md`](../docs/前端风格提示.md) → 视觉基调（代码注释里的「风格提示第 N 节」指的就是它）

视觉基调对齐参考仓库 [Mehado/supermall-demo](https://github.com/Mehado/supermall-demo)：
浅色底、大圆角卡片、毛玻璃吸顶头、`gray-900` 主按钮 + `amber` 强调色 + `emerald` 生鲜色，
并把它原来的 React + Tailwind 组件语言**重写为 Vue 3 + 原生 CSS 设计令牌**（不引 UI 框架）。

---

## 1. 快速开始

```bash
npm run dev      # 开发服务器 http://localhost:5178
npm run build    # 产物在 dist/
npm run preview  # 预览生产构建
```

依赖已经就绪（`node_modules` 从 `frontend` 同步而来，可离线直接跑）；重新安装也行：`npm install`。

### 后端转发（前端不写死域名）

`axios` 的 `baseURL` 全部留空，页面只用相对路径 `/api/...`，由 dev server 转发：

| 前缀 | 目标 | 说明 |
| --- | --- | --- |
| `/api/mall/**` | `http://localhost:8081` | 电商后端 |
| `/api/**`、`/img/**` | `http://localhost:8080` | 外卖后端 |

可用环境变量覆盖：`VITE_MALL_PROXY_TARGET`、`VITE_DELIVERY_PROXY_TARGET`。
后端没起来时页面不会白屏——接口失败会 toast，列表走空态占位。

### 演示账号

| 端 | 账号 | 密码 | 令牌头 |
| --- | --- | --- | --- |
| 商城买家 | `user1@qq.com` / `user2@qq.com` | `Mall1234` | `token` |
| 商城运营 | `admin@qq.com` | `Admin1234` | `mall-admin-token` |
| 外卖用户 | `user1@qq.com` / `user2@qq.com` | `Mall1234` | `token` |
| 外卖商家 | `merchant1@163.com` / `merchant2@163.com` / `merchant3@126.com` | `Mall1234` | `merchant-token` |
| 外卖骑手 | `rider1@gmail.com` / `rider2@gmail.com` | `Mall1234` | `delivery-token` |

> 登录页每个身份都能「一键填入」，切换角色不会互相踢下线。
> ⚠️ 种子商品默认是**下架**状态，联调加购/下单前先在后台把商品上架。

---

## 2. 端入口与路由地图

`/` 是主页面，左右两栏：左边是品牌介绍与优点，右边从上到下依次是「逛商城 / 点外卖 / 登录注册」，
商家端、骑手端与运营后台从登录入口按身份进入。也可以直接访问各端路由：

| 端 | 路由前缀 | 页面 |
| --- | --- | --- |
| 电商商城（买家） | `/shop/**` | 首页、搜索/分类、商品详情、购物车、确认订单、我的订单、订单详情、发表评价、我的评价、收藏、足迹、收货地址、个人中心 |
| 电商运营后台 | `/admin/**` | 销售看板、商品管理、商品新增/编辑（SPU+多图+规格 SKU）、分类、品牌、推荐位、订单发货、退款审核、评价、会员 |
| 外卖用户端 | `/food/**` | 点餐首页、商家列表、店铺点单、商品详情、购物车（按商家分组）、确认订单、我的订单、订单详情、个人中心 |
| 外卖商家端 | `/merchant/**` | 经营看板（待接单看板）、订单管理、商品管理、商品新增/编辑（含 SKU）、分类管理、店铺设置 |
| 外卖骑手端 | `/rider/**` | 抢单池、我的配送、骑手资料 |

公共页：`/login`（六端统一登录 + 角色 Tab）、`/register`（商城买家 / 外卖用户注册）、`/merchant/apply`（商家入驻）、`/rider/register`（骑手注册）、`/404`。
路由守卫按 `meta.role` 隔离：未登录访问受保护页面会跳到对应角色的登录页并带 `redirect`。

### 两端互不串门

- **导航与文案按端隔离**：外卖页面只出现外卖的入口与文案，商城页面只出现商城的，页脚也不会互相导流。
- **返回主页面**：每个端页面顶部与页脚各有一个「返回主页面」按钮，回到端入口由用户自己选择去哪个端。
- **登录页按来源端收敛身份**：从外卖进来只显示外卖的三种身份，从商城进来只显示商城的两种身份，
  需要时点「查看其它可登录身份」再展开。
- **页面上不出现实现细节**：接口路径、令牌、字段名、时间格式等只在代码注释与 README 里，界面文案一律面向使用者。

---

## 3. 目录结构

```
src/
├── api/            mall.js（电商 93 端点封装）、delivery.js（外卖 67 端点封装）
├── components/     Icon / ProductCard / SkuPicker / ModalSheet / Pager / CaptchaField
│                   / PasswordMeter / ImageUploader / AdminLayout / StatusPill …
├── layouts/        ShopLayout（商城外壳）、FoodLayout（外卖外壳）
├── router/         index.js + modules/{shop,admin,food,merchant,rider}.js（按端懒加载分包）
├── stores/         auth（五把 token）、cart（两端购物车角标）
├── styles/         tokens.css（设计令牌）、base.css（对齐 supermall-demo 的工具类）
├── utils/          request.js（5 个 axios 实例 + 统一拦截）、format.js（金额/时间/状态机映射）
└── views/          shop/ admin/ food/ merchant/ rider/ login/
```

### 3.1 品牌资源与浏览器标签图标

| 资源 | 位置 | 说明 |
| --- | --- | --- |
| 品牌 logo（页面内用） | `photo/logo/*.{png,svg,…}` | `src/lib/brand.js` 用 `import.meta.glob` 自动取第一个；取不到回退内置购物袋 SVG |
| 字标 | `photo/SuperMall-transparent.png` / `photo/SuperMall.png` | 登录页 / 主页面大标题 |
| 商城首页滚动图池 | `photo/marquee/*.{jpg,png,webp,avif}` | 电商首页字标后面的横向跑马灯；文件名随意（01、02… 控制顺序） |
| 外卖首页滚动图池 | `photo/marquee-food/*.{jpg,png,webp,avif}` | 同上，外卖首页用（两套图各放各的） |
| 空态 / 404 插图 | `photo/illustration/*.svg`（**内联渲染**，主色跟随 `--m-accent`） | `MEmpty` 的 `image` 属性 / 404 页；新增用 `scripts/import-illustration.mjs` |
| **标签页图标** | `public/favicon.ico`、`favicon.svg`、`favicon-16/32/192/512.png`、`apple-touch-icon.png` | 已在 `index.html` 里声明 |

**全站背景图**：`photo/portal-bg.jpg`（放上就生效，删掉自动退回品牌蓝柔光）。
统一由 `components/AppBackdrop.vue` 提供：铺图（`object-fit: cover`）+ 一层纵向渐变白纱；
虚化**由外壳按页分档**（2026-09-21 拍板）：

| 档位 | 页面 | 来源 |
| --- | --- | --- |
| 不虚化 | 主页面 `/`、登录、注册、商家入驻 | 这些页面不传 `blur` |
| 6px | 404 | `NotFound.vue` |
| 8px | 商城首页 `/shop` + 商城我的 `/shop/me`；外卖首页 `/food` + 外卖我的 `/food/me` | `ShopLayout` / `FoodLayout` `:blur="8"` |
| 10px | `/admin`、`/merchant/*`、`/rider/*`、`/platform` | `WorkbenchLayout :blur="10"` |

> 同外壳的子页**共享同一档**（所以 `/shop/me` 与 `/shop` 同为 8px）。
> 改档位要同步 `scripts/ui-smoke-backdrop.mjs` 里的 `expectBlur` 与 `docs/项目进度.md` #77 的档位表。

| 想调 | 位置 |
| --- | --- |
| 整体更清楚 / 更淡 | `AppBackdrop.vue` 里 `.bd-veil` 的三个 `rgba(255,255,255, N)`，N 越小图越明显 |
| 图的位置 | `.bd-img { object-position: center }` |
| 撤掉背景图 | 删 `frontend/photo/portal-bg.jpg` |

**例外**：小屏（`max-width: 900px`）一律不虚化 —— 大尺寸 fixed + blur 在手机上栅格化很贵，用 `!important` 压掉行内样式（所以背景脚本只在桌面 1440 宽下断言档位）。

> 原图留档在 `photo/_source/portal-bg.png`（1.63MB PNG → 148KB JPEG）。
> 回归：`node scripts/ui-smoke-backdrop.mjs`（13 个页面 / 14 项）。

> `photo/marquee/` 的图请**确认真实格式与扩展名一致**（把 WebP/AVIF 存成 `.png` 虽然多数浏览器会猜出来，但不稳）。

**原图留档**：`photo/_source/marquee/`（10 张）、`photo/_source/marquee-food/`（16 张）是**原始素材**，
不参与打包；`photo/marquee*` 里的 `01.jpg…` 是处理产物（JPEG + 4:5 + 自然序重命名）。
想换图/换比例就跑 `python scripts/optimize-images.py --src frontend/photo/_source/marquee --out frontend/photo/marquee`
（详见 `photo/_source/README.md`）。

> ⚠️ **备份/转移时别只打包 `src/`**：前端所有品牌图与商品图都在 **`frontend/photo/`**，漏了它页面会整片空图。
> 正确的一条命令：
> ```bash
> tar -czf frontend-backup-$(date +%Y%m%d-%H%M%S).tar.gz -C frontend src photo index.html vite.config.js package.json
> ```

图标是**从 logo 生成**的（不是手画的）：把 `photo/logo` 里的 logo 白底转透明、裁掉四周空白、
补成正方形，再导出 `favicon.ico`（16/32/48/64 多尺寸）、`favicon-16/32/192/512.png`、
以及白底的 `apple-touch-icon.png`（iOS 不支持透明，180×180）。

> 换了 logo 想重新生成：用 Python Pillow 按上面的规则导出到 `frontend/public/` 即可
> （favicon 会被浏览器长时间缓存，改完记得硬刷新 / 换个标签页试）。

---

## 4. 关键约定（照手册落地的地方）

1. **五把 token、五个请求头**：`token`（商城买家）/ `mall-admin-token` / `token`（外卖用户，与商城买家分开存）/ `merchant-token` / `delivery-token`，
   各端 session 独立存 `localStorage`，互不覆盖；401 时只清当前角色并跳它自己的登录页。
2. **统一返回体**：HTTP 恒 200，`code === 1` 才算成功；`code === 0` 弹 `msg` 并 reject。
3. **金额只信服务端**：购物车用 `selectedAmount`，订单用 `payAmount`，前端不做价格计算。
4. **一次性令牌**：下单、支付各取一次令牌（电商 10min/5min，外卖各一次），失败也会被消耗 → 页面每次操作都重新取令牌。
5. **时间格式分流**：请求体时间不带秒（`yyyy-MM-dd HH:mm`），外卖订单查询参数必须带秒（`yyyy-MM-dd HH:mm:ss`），统一在 `utils/format.js`。
6. **两套购物车语义**：电商 `{id}` 是购物车行 id、加购用 `skuId`、失效商品展示但不可选中；
   外卖按「用户 × 商家」分组、`number = 0` 即删除行、结算按商家拆单。
7. **状态机照抄手册**：订单 / 配送单按钮映射与流转一致（客户端不改状态，操作后重新拉取）。
8. **踩坑规避**：注册直接返回 token（不再调登录）、验证码一次性（失败自动重取）、无规格商品外卖传 `skuId: 0`、
   评价数为 0 显示「暂无评价」而不是 5 分、晒图先传 OSS 拿 URL 再提交。

### 明确不做

- 优惠券 / 满减 / 折扣（本期无接口，`discountAmount` 恒为 0）
- 真实支付（只有 Mock 支付）、真实物流
- 外卖 WebSocket 推送（沿用轮询：商家看板 30s、骑手抢单池 15s）

---

## 5. 部署提示

`history` 模式 SPA，Nginx 需要：

```nginx
location / { try_files $uri $uri/ /index.html; }
location /api/mall/ { proxy_pass http://127.0.0.1:8081; }
location /api/      { proxy_pass http://127.0.0.1:8080; }
location /img/      { proxy_pass http://127.0.0.1:8080; }
```
