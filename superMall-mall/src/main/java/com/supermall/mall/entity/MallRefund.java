package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退货退款申请（mall_refund）。
 *
 * <p>整单退款：申请时把订单置为 REFUNDING（条件更新即互斥锁，防并发重复申请），
 * 并记录 {@code previousStatus}；驳回时原样回退，通过时置为 REFUNDED 并回补库存。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallRefund implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 退款单号 */
    private String refundNumber;

    private Long orderId;

    private String orderNumber;

    private Long userId;

    /** MallRefundType：1 仅退款 2 退货退款 */
    private Integer type;

    private String reason;

    private String description;

    /** 退款金额（= 订单实付） */
    private BigDecimal amount;

    /** MallRefundStatus */
    private String status;

    /** 申请前的订单状态，驳回时回退用 */
    private String previousStatus;

    private String adminRemark;

    private LocalDateTime auditTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
