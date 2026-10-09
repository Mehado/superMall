package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车单行。
 *
 * <p>{@code valid=false} 的是「失效商品」：商品下架 / 规格失效 / 售罄 / 库存不足。
 * 它们在列表里照常返回并带 {@code invalidReason}，但**不参与结算金额统计**，
 * 也不能被选中。这样用户能看到自己的东西还在，而不是「凭空消失」。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "购物车行")
public class MallCartItemVO implements Serializable {

    private Long id;

    private Long productId;

    private Long skuId;

    private Integer number;

    /** 1 选中 0 未选 */
    private Integer selected;

    private String productName;

    private String subtitle;

    /** 列表主图：优先 SKU 图，没有则用商品主图 */
    private String image;

    private String skuCode;

    @Schema(description = "规格描述，如「白 / M」；无规格商品为空")
    private String specText;

    @Schema(description = "当前 SKU 单价（实时价，非加购时快照）")
    private BigDecimal price;

    @Schema(description = "当前 SKU 库存")
    private Integer stock;

    @Schema(description = "商品状态 1 上架 0 下架")
    private Integer productStatus;

    @Schema(description = "SKU 状态 1 启用 0 停用")
    private Integer skuStatus;

    @Schema(description = "小计 = 单价 × 数量")
    private BigDecimal subtotal;

    @Schema(description = "是否有效（false 时不能选中、不计入结算）")
    private Boolean valid;

    @Schema(description = "失效原因；valid=true 时为 null")
    private String invalidReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
