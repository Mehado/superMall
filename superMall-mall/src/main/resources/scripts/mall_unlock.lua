-- 分布式锁释放脚本：值匹配才删除，防止 TTL 过期后误删别人的锁（电商版）
-- KEYS[1] = 锁 key（mall:cache:lock:{业务 key}）
-- ARGV[1] = 持有锁的标识（线程名）
-- 返回值：1=删除成功，0=值不匹配（锁已过期或被别人持有）
--
-- 为什么用 Lua：GET + DEL 两步不原子，中间锁若过期并被别人抢走，就会误删别人的锁。
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
