-- 库存回补脚本：key 存在才加回，防止把负库存写进去（电商版）
-- KEYS[1] = mall:stock:sku:{skuId}
-- ARGV[1] = 回补数量
-- 返回值：加回后的库存；key 不存在返回 -1
if redis.call('exists', KEYS[1]) == 1 then
    return redis.call('incrby', KEYS[1], ARGV[1])
else
    return -1
end
