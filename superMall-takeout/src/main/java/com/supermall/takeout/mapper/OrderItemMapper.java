package com.supermall.takeout.mapper;

import com.supermall.takeout.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    void batchInsert(@Param("list") List<OrderItem> list);

    List<OrderItem> listByOrderId(Long orderId);

    List<OrderItem> listByOrderIds(@Param("orderIds") List<Long> orderIds);
}
