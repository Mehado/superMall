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
 * 提交订单结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "提交订单结果")
public class MallOrderSubmitVO implements Serializable {

    private Long id;

    private String orderNumber;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal freightAmount;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    private LocalDateTime createTime;

    @Schema(description = "支付截止时间，超时会自动取消")
    private LocalDateTime expireTime;
}
