package com.supermall.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表。
 * status 取值见 {@link com.supermall.takeout.enumeration.OrderStatus}，payStatus 见 PayStatus。
 * 商家隔离字段 merchantId。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orders implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 订单号 */
    private String number;

    /** 下单幂等令牌（与 user_id 组成唯一索引） */
    private String submitToken;

    private Long userId;

    private Long merchantId;

    private Long addressId;

    /** 订单主状态 */
    private String status;

    /** 支付状态 */
    private String payStatus;

    /** 支付方式：MOCK / WECHAT / ALIPAY */
    private String payMethod;

    /** 商品金额 */
    private BigDecimal amount;

    /** 配送费 */
    private BigDecimal deliveryFee;

    private String remark;

    private String consignee;

    private String phone;

    private String address;

    private LocalDateTime orderTime;

    private LocalDateTime checkoutTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private LocalDateTime deliveryTime;

    private LocalDateTime estimatedDeliveryTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
