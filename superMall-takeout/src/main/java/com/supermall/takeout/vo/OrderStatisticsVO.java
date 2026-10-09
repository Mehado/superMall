package com.supermall.takeout.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商家端订单状态统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsVO implements Serializable {

    private Integer pendingPayment;

    private Integer paid;

    private Integer merchantAccepted;

    private Integer waitingDelivery;

    private Integer delivering;

    private Integer completed;

    /** 今日有效订单数（不含已取消 / 待付款） */
    private Integer todayOrders;

    /** 今日营业额（不含已取消 / 待付款） */
    private Double todayAmount;
}
