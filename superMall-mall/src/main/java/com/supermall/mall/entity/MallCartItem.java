package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 购物车明细（mall_cart_item）。
 *
 * <p>三件关键事：
 * <ul>
 *   <li><b>不存价格快照</b> —— 列表每次都 JOIN {@code mall_sku} 取当前价，
 *       满足「价格实时计算」；</li>
 *   <li><b>加购粒度是 SKU</b> —— 79 元的 M 码和 82 元的 L 码在购物车里是两行
 *       （唯一键 {@code (user_id, sku_id)}）；</li>
 *   <li><b>没有购物车头表</b> —— 自营不引入商家维度，一张明细表就够了。</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallCartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** sys_user.id（与外卖共享的账号） */
    private Long userId;

    private Long productId;

    private Long skuId;

    private Integer number;

    /** 1 选中 0 未选（结算只看「有效且选中」的行） */
    private Integer selected;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
