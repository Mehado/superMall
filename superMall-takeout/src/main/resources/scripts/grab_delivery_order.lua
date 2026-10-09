-- 配送员抢单闸门：同一配送单只放行一个配送员
-- KEYS[1] = 抢单标记 key（delivery:grab:{orderId}）
-- ARGV[1] = 配送员 id
-- ARGV[2] = 标记 TTL（秒）
-- 返回值：1=抢到闸门，0=已被别人抢走
if redis.call('exists', KEYS[1]) == 1 then
    return 0
end
redis.call('set', KEYS[1], ARGV[1], 'EX', tonumber(ARGV[2]))
return 1
