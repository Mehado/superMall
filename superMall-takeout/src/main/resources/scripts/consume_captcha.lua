-- 图形验证码消费脚本：取值 + 删除 原子执行
-- KEYS[1] = captcha:{key}
-- 返回值：答案字符串；key 不存在（过期/已用过）时返回 nil
--
-- 为什么不用 GETDEL：本项目 Redis 是 5.0.x，GETDEL 要 6.2+ 才有。
-- 用 Lua 既规避版本问题，又保证「取值 + 删除」原子，验证码无法被并发重放。
local value = redis.call('get', KEYS[1])
if value then
    redis.call('del', KEYS[1])
end
return value
