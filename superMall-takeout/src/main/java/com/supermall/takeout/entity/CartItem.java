package com.supermall.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车明细
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long cartId;

    private Long userId;

    private Long merchantId;

    private Long productId;

    /** 无规格时为 0 */
    private Long skuId;

    private String productName;

    private String skuName;

    private String image;

    /** 下单时价格快照 */
    private BigDecimal price;

    private Integer number;

    private LocalDateTime createTime;
}
