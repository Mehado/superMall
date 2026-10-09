package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 热销商品（TOP10 柱状图）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热销商品")
public class MallTopProductVO implements Serializable {

    private Long productId;

    private String productName;

    @Schema(description = "销量（件数）")
    private Integer quantity;

    @Schema(description = "成交额")
    private BigDecimal amount;
}
