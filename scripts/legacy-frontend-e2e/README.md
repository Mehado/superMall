# 旧前端（frontend）的端到端脚本（已归档）

这三个脚本原本在 `frontend/scripts/`，用于验收**旧的外卖三端合一前端**：
- `acceptance.mjs` —— 登录/注册/守卫/分包/入口（72 项）
- `business-e2e.mjs` —— 下单→支付→接单→备餐→抢单→取餐→送达（18 项）
- `e2e-lib.mjs` —— 公共工具（读 `data-captcha-key` + Redis 取验证码答案）

`frontend` 已被新的五端合一前端 `frontend/` 取代，脚本的 DOM 选择器/路由针对旧界面，
**不能直接对 `frontend/` 跑**，仅作参考保留（可直接删除）。

要复用时：把 `BASE` 指到新前端的地址，并把选择器/路由按 `frontend/src` 重写。
