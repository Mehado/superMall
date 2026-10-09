package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 支付请求（Mock 支付）。
 *
 * <p>与外卖一致：用 Redis 令牌保证支付幂等，令牌一次性消费。
 */
@Data
@Schema(description = "支付请求")
public class MallPaymentDTO implements Serializable {

    @NotBlank(message = "订单号不能为空")
    private String orderNumber;

    @NotBlank(message = "支付令牌不能为空")
    @Schema(description = "支付令牌，先调 GET /api/mall/user/payment/token?orderNumber=xxx 获取")
    private String payToken;
}
