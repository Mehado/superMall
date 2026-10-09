package com.supermall.takeout.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class PaymentDTO implements Serializable {

    @NotBlank(message = "订单号不能为空")
    private String orderNumber;

    @NotBlank(message = "支付令牌不能为空")
    private String payToken;
}
