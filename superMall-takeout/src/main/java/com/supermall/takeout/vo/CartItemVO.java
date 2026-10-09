package com.supermall.takeout.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemVO implements Serializable {

    private Long id;

    private Long productId;

    private Long skuId;

    private String productName;

    private String skuName;

    private String image;

    private BigDecimal price;

    private Integer number;

    /** 小计 */
    private BigDecimal amount;
}
