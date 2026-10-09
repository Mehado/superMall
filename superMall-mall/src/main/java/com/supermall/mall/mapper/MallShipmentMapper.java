package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallShipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 物流单（mall_shipment）。
 */
@Mapper
public interface MallShipmentMapper {

    int insert(MallShipment shipment);

    MallShipment getByOrderId(@Param("orderId") Long orderId);

    List<MallShipment> listByOrderIds(@Param("orderIds") List<Long> orderIds);
}
