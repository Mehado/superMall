package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车视图：明细 + 汇总。
 *
 * <p>汇总口径：
 * <ul>
 *   <li>{@code totalNumber} 所有行的数量之和（含失效）</li>
 *   <li>{@code selectedNumber} / {@code selectedAmount} 只统计「有效且选中」的行 ——
 *       这就是结算页要付的钱</li>
 *   <li>{@code allSelected} 是否所有**有效**行都已选中（全选按钮的勾选态）</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "购物车")
public class MallCartVO implements Serializable {

    private List<MallCartItemVO> items;

    @Schema(description = "购物车行数（含失效）")
    private Integer totalItems;

    @Schema(description = "商品总件数（含失效）")
    private Integer totalNumber;

    @Schema(description = "有效行数")
    private Integer validCount;

    @Schema(description = "失效行数")
    private Integer invalidCount;

    @Schema(description = "有效且选中的行数")
    private Integer selectedCount;

    @Schema(description = "有效且选中的件数")
    private Integer selectedNumber;

    @Schema(description = "有效且选中的合计金额")
    private BigDecimal selectedAmount;

    @Schema(description = "是否所有有效行都已选中（没有有效行时为 false）")
    private Boolean allSelected;
}
