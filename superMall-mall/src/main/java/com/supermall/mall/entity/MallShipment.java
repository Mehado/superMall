package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物流单（mall_shipment）—— 与订单一对一。
 *
 * <p>自营发货只有承运商 + 物流单号，没有骑手抢单那一套，所以比外卖的 delivery_order 简单得多。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallShipment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 唯一键，一单一张物流单 */
    private Long orderId;

    private String orderNumber;

    /** 物流公司，如「顺丰速运」 */
    private String company;

    /** 物流单号 */
    private String trackingNo;

    private LocalDateTime shipTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
