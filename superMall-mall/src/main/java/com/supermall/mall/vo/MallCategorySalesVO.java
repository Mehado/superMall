package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 品类销售占比（饼图）。
 *
 * <p>按**顶级分类**聚合（商品可能挂在 3 级分类下，这里向上归到根分类）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "品类销售占比")
public class MallCategorySalesVO implements Serializable {

    private Long categoryId;

    private String categoryName;

    @Schema(description = "成交额")
    private BigDecimal amount;

    @Schema(description = "销量（件数）")
    private Integer quantity;

    @Schema(description = "金额占比 %（服务端算好，前端直接画饼图）")
    private BigDecimal percent;
}
