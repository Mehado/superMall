-- 幂等令牌消费脚本：检查 + 删除 原子执行（电商版）
-- KEYS[1] = mall:order:submit:token:{token} 或 mall:pay:token:{token}
-- ARGV[1] = 当前用户 id
-- 返回值：1=消费成功，0=重复提交 / 令牌不属于该用户
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
