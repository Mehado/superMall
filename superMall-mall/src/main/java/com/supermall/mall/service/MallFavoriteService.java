package com.supermall.mall.service;

import com.supermall.result.PageResult;

/**
 * 商品收藏夹（需登录）。
 *
 * <p>所有方法都以 {@code userId} 为参数、由 Controller 从登录态注入，
 * 不接受前端传 userId —— 否则可以看别人的收藏夹。
 */
public interface MallFavoriteService {

    /** 收藏（幂等：已收藏再调一次不报错，也不产生重复行） */
    void add(Long userId, Long productId);

    /** 取消收藏（幂等：本来就没收藏也不报错） */
    void remove(Long userId, Long productId);

    /** 是否已收藏（商品详情页用） */
    boolean exists(Long userId, Long productId);

    /** 收藏总数 */
    int count(Long userId);

    /**
     * 收藏列表（商品卡片，按收藏时间倒序）。
     *
     * <p>**不过滤下架商品**：下架了也要能看到并标灰（{@code product.status = 0}），
     * 否则用户会觉得收藏"凭空消失"。
     */
    PageResult page(Long userId, Integer page, Integer pageSize);
}
