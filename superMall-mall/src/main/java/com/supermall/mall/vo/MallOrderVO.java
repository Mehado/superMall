package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图（买家 / 运营后台共用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单")
public class MallOrderVO implements Serializable {

    private Long id;

    private String orderNumber;

    private Long userId;

    private String status;

    @Schema(description = "状态中文描述，如「待付款」")
    private String statusDesc;

    private String payStatus;

    private String payMethod;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal freightAmount;

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

    private String cancelReason;

    private LocalDateTime cancelTime;

    private LocalDateTime payTime;

    private LocalDateTime deliveryTime;

    private LocalDateTime receiveTime;

    /** 支付截止时间；只有待付款时才有意义 */
    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Schema(description = "订单明细")
    private List<MallOrderItemVO> items;

    @Schema(description = "物流信息；未发货时为 null")
    private MallShipmentVO shipment;

    @Schema(description = "退款申请；没有则为 null")
    private MallRefundVO refund;
}
