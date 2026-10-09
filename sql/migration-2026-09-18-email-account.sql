-- ============================================================
-- 迁移：登录账号改为邮箱 + 密码强度（2026-09-18）
-- ------------------------------------------------------------
-- 背景：
--   登录新增「邮箱格式 + 常用邮箱后缀」校验，密码要求 8-32 位且含大小写字母与数字。
--   旧的演示账号（user1 / merchant1 / rider1 ...）既不是邮箱，密码也是 6 位纯数字 123456，
--   不迁移的话将无法登录。
--
-- 本脚本做三件事（幂等，可重复执行）：
--   1. 7 个演示账号改名成邮箱
--   2. 其余仍是非邮箱格式的账号，补一个默认后缀（保证仍能登录）
--   3. 仍是旧演示密码（123456）的账号，统一改成 Mall1234
--
-- 执行完会打印一份账号清单供核对。
-- ============================================================

-- ---------- 1. 演示账号 → 邮箱 ----------

-- 用户
UPDATE sys_user SET username = 'user1@qq.com'   WHERE username = 'user1';
UPDATE sys_user SET username = 'user2@qq.com'   WHERE username = 'user2';
-- 商家
UPDATE merchant SET username = 'merchant1@163.com' WHERE username = 'merchant1';
UPDATE merchant SET username = 'merchant2@163.com' WHERE username = 'merchant2';
UPDATE merchant SET username = 'merchant3@126.com' WHERE username = 'merchant3';
-- 配送员
UPDATE deliveryman SET username = 'rider1@gmail.com' WHERE username = 'rider1';
UPDATE deliveryman SET username = 'rider2@gmail.com' WHERE username = 'rider2';

-- ---------- 2. 兜底：其余非邮箱账号补默认后缀 ----------
-- 手工注册过的账号（如联调时创建的 mehado）不是邮箱，补后缀后仍可通过前端邮箱校验。
-- 幂等：执行一次后所有 username 都含 @，第二次不会再匹配。

UPDATE sys_user    SET username = CONCAT(username, '@qq.com')    WHERE username NOT LIKE '%@%';
UPDATE merchant    SET username = CONCAT(username, '@163.com')   WHERE username NOT LIKE '%@%';
UPDATE deliveryman SET username = CONCAT(username, '@gmail.com') WHERE username NOT LIKE '%@%';

-- ---------- 3. 旧演示密码 123456 → Mall1234 ----------
-- 按「旧 hash 值」匹配，覆盖所有仍在使用 123456 的账号（含手工注册的）。
-- 自己改过密码的账号不会被匹配到，保留原密码。
-- 注意：若那些自定义密码不满足新规则（8 位 + 大小写 + 数字），前端会拦在格式校验上，
--       需要本人走「修改密码」流程，或由管理员按下方的语句重置。

-- 注意：这里刻意【不用】用户变量（SET @hash = ...）。
-- 用户变量的 collation 是 IMPLICIT，与列的 IMPLICIT 撞车会报
-- "Illegal mix of collations"；而字面量是 COERCIBLE，会自动让位给列的 collation，所以直接内联。
UPDATE sys_user    SET password = '$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6'
  WHERE password = '$2a$10$svb1PLFBGkwMQOMapKuT1uLSj8X.ZJpRpy8nyiUWpjiwH/plBbo.y';
UPDATE merchant    SET password = '$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6'
  WHERE password = '$2a$10$svb1PLFBGkwMQOMapKuT1uLSj8X.ZJpRpy8nyiUWpjiwH/plBbo.y';
UPDATE deliveryman SET password = '$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6'
  WHERE password = '$2a$10$svb1PLFBGkwMQOMapKuT1uLSj8X.ZJpRpy8nyiUWpjiwH/plBbo.y';

-- ---------- 核对 ----------
-- 用三条独立 SELECT（不用 UNION）：中文字面量与列的 collation 不同，
-- 在 UNION 里混用会报 Illegal mix of collations。
SELECT id, username, nickname   AS name FROM sys_user    ORDER BY id;
SELECT id, username, shop_name  AS name FROM merchant    ORDER BY id;
SELECT id, username, name              FROM deliveryman ORDER BY id;

-- 如果某个自定义密码的账号登不进去，用下面这句把它重置成 Mall1234：
-- UPDATE sys_user SET password = '$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6'
--   WHERE username = '要重置的邮箱';
