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
 * 会员（买家）视图 —— 运营后台会员管理用。
 *
 * <p>账号来自共享表 {@code sys_user}，统计字段（订单数 / 消费额 / 评价数）由
 * {@code mall_order} / {@code mall_review} 子查询算出（单页数据量小，不值得再建汇总表）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会员")
public class MallMemberVO implements Serializable {

    private Long id;

    @Schema(description = "登录邮箱")
    private String username;

    private String nickname;

    private String phone;

    private String avatar;

    @Schema(description = "1 启用 0 禁用")
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /* ---------- 统计 ---------- */

    @Schema(description = "订单总数（含未支付/已取消）")
    private Integer orderCount;

    @Schema(description = "已支付订单数")
    private Integer paidOrderCount;

    @Schema(description = "累计消费（已支付实付之和，不含已退款）")
    private BigDecimal totalSpent;

    @Schema(description = "评价数")
    private Integer reviewCount;

    @Schema(description = "最近一次支付时间")
    private LocalDateTime lastOrderTime;
}
