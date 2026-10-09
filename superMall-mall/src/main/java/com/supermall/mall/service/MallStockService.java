package com.supermall.mall.service;

import com.supermall.mall.entity.MallOrderItem;

import java.util.List;

/**
 * 电商库存服务：Redis 预扣 + DB 条件扣减，两层防超卖。
 *
 * <p>与外卖同思路，但电商的商品一定有 SKU（无规格商品也有默认 SKU），
 * 所以库存只挂在 SKU 上，key 是 {@code mall:stock:sku:{skuId}}。
 *
 * <pre>
 * 下单   deductRedis（挡并发） → deductDb（where stock >= ?，最终一致） → refreshAggregates
 * 取消/退款  releaseDb → refreshAggregates → releaseRedis
 * </pre>
 *
 * <p>拆分 Redis 与 DB 两步，是为了让调用方能在事务回滚时手动回补 Redis
 * （Redis 不参与数据库事务）。
 */
public interface MallStockService {

    /** 启动预热：把 mall_sku.stock 全量写入 Redis */
    void warmUpAll();

    /** 商品保存后同步该商品下所有 SKU 的库存到 Redis（SKU 是重建的，id 会变） */
    void syncProductSkuStock(Long productId);

    /** 单个 SKU 同步（改库存/测试用） */
    void syncSkuStock(Long skuId, Integer stock);

    /** Redis 预扣：任一 SKU 不足则回补已扣部分并抛异常 */
    void deductRedis(List<MallOrderItem> items);

    /** Redis 回补（key 不存在则跳过） */
    void releaseRedis(List<MallOrderItem> items);

    /** DB 条件扣减（{@code stock >= number}），返回 0 抛「库存不足」 */
    void deductDb(List<MallOrderItem> items);

    /** DB 回补 */
    void releaseDb(List<MallOrderItem> items);

    /** 由 SKU 重算 SPU 的冗余字段（stock / price_low / price_high） */
    void refreshAggregates(List<MallOrderItem> items);

    /** 调整销量：delta>0 支付成功累加；delta<0 退款通过扣回（SKU + SPU 同步） */
    void adjustSales(List<MallOrderItem> items, int delta);
}
