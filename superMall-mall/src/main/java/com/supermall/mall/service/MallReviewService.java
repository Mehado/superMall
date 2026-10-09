package com.supermall.mall.service;

import com.supermall.mall.dto.MallReviewFollowDTO;
import com.supermall.mall.dto.MallReviewSubmitDTO;
import com.supermall.mall.vo.MallOrderReviewVO;
import com.supermall.mall.vo.MallReviewStatsVO;
import com.supermall.result.PageResult;

/**
 * 商品评价（买家发表 / 商品详情展示 / 运营后台管理）。
 *
 * <p>规则：
 * <ul>
 *   <li>只有**已完成**订单能评价，粒度是订单明细（一个明细只能评一次）；</li>
 *   <li>可以追评一次（不改变评分）；</li>
 *   <li>发表 / 删除后重算商品的 {@code rating} / {@code reviewCount} / {@code goodRate}；</li>
 *   <li>匿名评价对外不暴露昵称或邮箱。</li>
 * </ul>
 */
public interface MallReviewService {

    /** 批量评价一个订单，返回实际写入的条数 */
    int submit(Long userId, MallReviewSubmitDTO dto);

    /** 追评（一次） */
    void follow(Long userId, Long reviewId, MallReviewFollowDTO dto);

    /** 我的评价分页 */
    PageResult pageMine(Long userId, Integer page, Integer pageSize);

    /** 某订单的评价情况（哪些明细已评 / 未评） */
    MallOrderReviewVO orderReviews(Long userId, String orderNumber);

    /* ---------------- 公开（商品详情页） ---------------- */

    PageResult pageByProduct(Long productId, Integer rating, Boolean hasImage, Integer page, Integer pageSize);

    MallReviewStatsVO stats(Long productId);

    /* ---------------- 运营后台 ---------------- */

    PageResult pageByAdmin(Long productId, Long userId, Integer rating, Integer page, Integer pageSize);

    /** 删除违规评价，并重算商品评价聚合 */
    void delete(Long reviewId);
}
