package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品浏览足迹（mall_footprint）。
 *
 * <p>唯一键 {@code (user_id, product_id)} + {@code ON DUPLICATE KEY UPDATE}：
 * 重复浏览同一商品只累加 {@code visitCount} 并刷新 {@code updateTime}，
 * 不会把足迹刷成一堆重复行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallFootprint implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** sys_user.id */
    private Long userId;

    private Long productId;

    /** 浏览次数 */
    private Integer visitCount;

    /** 首次浏览 */
    private LocalDateTime createTime;

    /** 最近浏览（列表按它倒序） */
    private LocalDateTime updateTime;
}
