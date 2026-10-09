package com.supermall.takeout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户提交订单：金额由服务端根据购物车重新计算，不信任前端。
 */
@Data
public class OrderSubmitDTO implements Serializable {

    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    @NotNull(message = "商家id不能为空")
    private Long merchantId;

    private String remark;

    /** 期望送达时间 */
    private LocalDateTime estimatedDeliveryTime;

    /** 支付方式：MOCK/WECHAT/ALIPAY */
    private String payMethod;

    @NotBlank(message = "请先获取下单令牌")
    private String token;
}
