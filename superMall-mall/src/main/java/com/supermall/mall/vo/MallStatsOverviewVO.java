package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 运营看板总览（卡片数据）。
 *
 * <p>口径：**成交额 / 订单数只看 pay_status = PAID 的订单**（未支付、已退款都不算）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "运营看板总览")
public class MallStatsOverviewVO implements Serializable {

    @Schema(description = "累计成交额（已支付实付之和，不含已退款）")
    private BigDecimal totalSales;

    @Schema(description = "已支付订单数")
    private Integer paidOrders;

    @Schema(description = "订单总数（含未支付 / 已取消）")
    private Integer totalOrders;

    @Schema(description = "今日成交额")
    private BigDecimal todaySales;

    @Schema(description = "今日已支付订单数")
    private Integer todayOrders;

    @Schema(description = "会员数")
    private Integer memberCount;

    @Schema(description = "在售商品数")
    private Integer onSaleProducts;

    @Schema(description = "待发货订单数")
    private Integer pendingDelivery;

    @Schema(description = "待审核退款数")
    private Integer refunding;

    @Schema(description = "评价总数")
    private Integer reviewCount;
}
