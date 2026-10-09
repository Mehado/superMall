package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细项（下单快照）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单明细项")
public class MallOrderItemVO implements Serializable {

    private Long id;

    private Long productId;

    private Long skuId;

    private String productName;

    private String skuCode;

    private String image;

    @Schema(description = "规格描述，如「白 / M」")
    private String specText;

    private BigDecimal price;

    private Integer number;

    private BigDecimal totalAmount;
}
