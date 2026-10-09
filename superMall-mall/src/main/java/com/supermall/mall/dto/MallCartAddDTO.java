package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 加入购物车。
 *
 * <p>只收 「SKU + 数量」：商品 id 由服务端按 SKU 反查，
 * 防止前端传一个不匹配的 productId 造成数据错配。
 */
@Data
@Schema(description = "加入购物车")
public class MallCartAddDTO implements Serializable {

    @NotNull(message = "skuId 不能为空")
    @Schema(description = "SKU id（无规格商品也有默认 SKU）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 999, message = "单个商品最多加购 999 件")
    @Schema(description = "加购数量，默认 1", example = "1")
    private Integer number;
}
