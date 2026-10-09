package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单（mall_order）。
 *
 * <p>设计要点：
 * <ul>
 *   <li>金额四件套：商品总额 / 优惠 / 运费 / 实付，全部由服务端计算，不信任前端；</li>
 *   <li>收货信息是**下单时的快照**（地址改删都不影响历史订单）；</li>
 *   <li>状态流转全部走条件更新，配合 {@code (user_id, submit_token)} 唯一键做下单幂等兜底。</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 订单号（对外唯一标识，规则见 MallOrderServiceImpl#generateOrderNumber） */
    private String orderNumber;

    private Long userId;

    /** MallOrderStatus */
    private String status;

    /** MallPayStatus */
    private String payStatus;

    private String payMethod;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal freightAmount;

    /** 实付 = 商品总额 - 优惠 + 运费 */
    private BigDecimal payAmount;

    /* ---------- 收货信息快照 ---------- */

    private String receiverName;

    private String receiverPhone;

    private String receiverProvince;

    private String receiverCity;

    private String receiverDistrict;

    private String receiverAreaCode;

    private String receiverDetail;

    private String remark;

    /** 下单幂等令牌，与 user_id 组成唯一键 */
    private String submitToken;

    private String cancelReason;

    private LocalDateTime cancelTime;

    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime deliveryTime;

    /** 收货时间 */
    private LocalDateTime receiveTime;

    /** 支付截止时间（超时关单任务用） */
    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
