package com.supermall.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细（下单时保存商品快照）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private Long merchantId;

    private Long productId;

    private Long skuId;

    private String productName;

    private String skuName;

    private String image;

    private BigDecimal price;

    private Integer number;

    /** 小计 = price * number */
    private BigDecimal amount;
}
