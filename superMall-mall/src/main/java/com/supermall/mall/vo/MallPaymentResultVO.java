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
 * 支付结果（Mock 支付，字段结构与外卖保持一致，方便前端复用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "支付结果")
public class MallPaymentResultVO implements Serializable {

    private String orderNumber;

    private BigDecimal payAmount;

    private LocalDateTime payTime;

    private String nonceStr;

    private String paySign;

    private String timeStamp;

    private String signType;

    @Schema(description = "模拟的 prepay_id")
    private String packageStr;
}
