package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 提交订单。
 *
 * <p>只收「地址 + 备注 + 幂等令牌」：商品来自购物车里「有效且选中」的行，
 * 金额由服务端重算 —— 前端传不了价格，也就改不了钱。
 */
@Data
@Schema(description = "提交订单")
public class MallOrderSubmitDTO implements Serializable {

    @NotNull(message = "请选择收货地址")
    @Schema(description = "收货地址 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long addressId;

    @Schema(description = "买家备注")
    private String remark;

    @NotBlank(message = "下单令牌不能为空")
    @Schema(description = "下单令牌，先调 GET /api/mall/user/orders/token 获取",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String submitToken;
}
