package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细（mall_order_item）。
 *
 * <p>是**下单那一刻的商品快照**：商品改名、改价、下架都不影响历史订单的展示与金额。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private String orderNumber;

    private Long productId;

    private Long skuId;

    private String productName;

    private String skuCode;

    private String image;

    /** 规格描述，如「白 / M」；无规格商品为空 */
    private String specText;

    private BigDecimal price;

    private Integer number;

    /** 小计 = price × number */
    private BigDecimal totalAmount;
}
