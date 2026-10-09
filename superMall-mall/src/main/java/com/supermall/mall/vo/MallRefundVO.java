package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款申请视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "退款申请")
public class MallRefundVO implements Serializable {

    private Long id;

    private String refundNumber;

    private Long orderId;

    private String orderNumber;

    private Long userId;

    @Schema(description = "1 仅退款；2 退货退款")
    private Integer type;

    @Schema(description = "退款类型描述")
    private String typeDesc;

    private String reason;

    private String description;

    private BigDecimal amount;

    private String status;

    @Schema(description = "退款状态描述")
    private String statusDesc;

    @Schema(description = "申请前的订单状态")
    private String previousStatus;

    private String adminRemark;

    private LocalDateTime auditTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Schema(description = "关联订单（列表接口为 null，详情接口才带）")
    private MallOrderVO order;
}
