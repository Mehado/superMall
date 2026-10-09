package com.supermall.takeout.service;

import com.supermall.takeout.entity.CartItem;
import com.supermall.takeout.entity.OrderItem;

import java.util.List;

/**
 * 库存服务：Redis + Lua 保证「检查 + 扣减」原子性，防止超卖。
 * 返回值约定：1=成功，-1=库存不足，-2=未预热。
 */
public interface StockService {

    /** 下单时预扣库存（购物车维度） */
    void deductStock(List<CartItem> items);

    /** 取消 / 超时关单时回补库存（订单明细维度） */
    void releaseStock(List<OrderItem> items);

    /** 应用启动预热全部库存到 Redis */
    void warmUpAll();

    /** 商品库存变更后同步 Redis */
    void syncProductStock(Long productId, Integer stock);

    /** SKU 库存变更后同步 Redis */
    void syncSkuStock(Long skuId, Integer stock);
}
