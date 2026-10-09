package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 销售额趋势点（折线图）。
 *
 * <p>日：{@code date} 形如 {@code 2026-09-21}；月：{@code 2026-09}。
 * 没有成交的日期会补 0（由服务层补齐），保证前端折线不断档。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "销售额趋势点")
public class MallSalesTrendVO implements Serializable {

    @Schema(description = "日期（日：yyyy-MM-dd；月：yyyy-MM）")
    private String date;

    @Schema(description = "成交额")
    private BigDecimal sales;

    @Schema(description = "已支付订单数")
    private Integer orders;
}
