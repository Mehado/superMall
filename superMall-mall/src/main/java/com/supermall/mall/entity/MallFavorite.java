package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品收藏（mall_favorite）。
 *
 * <p>唯一键 {@code (user_id, product_id)}：同一用户对同一商品只有一条记录，
 * 重复收藏是幂等的（不会产生重复行）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallFavorite implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** sys_user.id（与外卖共享的账号） */
    private Long userId;

    private Long productId;

    private LocalDateTime createTime;
}
