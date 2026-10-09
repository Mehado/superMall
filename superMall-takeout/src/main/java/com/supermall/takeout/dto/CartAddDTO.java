package com.supermall.takeout.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class CartAddDTO implements Serializable {

    @NotNull(message = "商家id不能为空")
    private Long merchantId;

    @NotNull(message = "商品id不能为空")
    private Long productId;

    /** 无规格传 0 或不传 */
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer number;
}
