package com.supermall.takeout.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 一个商家一辆购物车，返回时按商家分组。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartVO implements Serializable {

    private Long cartId;

    private Long merchantId;

    private String shopName;

    private List<CartItemVO> items;

    /** 该商家购物车合计金额 */
    private BigDecimal totalAmount;
}
