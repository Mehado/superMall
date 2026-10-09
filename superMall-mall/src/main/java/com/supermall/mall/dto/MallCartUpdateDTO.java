package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改购物车数量（数量上限由 SKU 库存决定，服务端校验）。
 */
@Data
@Schema(description = "修改购物车数量")
public class MallCartUpdateDTO implements Serializable {

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 999, message = "单个商品最多 999 件")
    private Integer number;
}
