package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细（mall_order_item）。
 */
@Mapper
public interface MallOrderItemMapper {

    void batchInsert(@Param("items") List<MallOrderItem> items);

    List<MallOrderItem> listByOrderId(@Param("orderId") Long orderId);

    /** 批量查：订单列表页一次查完所有明细，避免 N+1 */
    List<MallOrderItem> listByOrderIds(@Param("orderIds") List<Long> orderIds);
}
