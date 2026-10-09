package com.supermall.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配送单：与订单一一对应，状态独立维护。
 * status 取值见 {@link com.supermall.takeout.enumeration.DeliveryStatus}。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private Long merchantId;

    /** 抢单成功后的配送员 id */
    private Long deliverymanId;

    private String status;

    private LocalDateTime acceptTime;

    private LocalDateTime pickupTime;

    private LocalDateTime deliveredTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
