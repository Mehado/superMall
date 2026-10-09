package com.supermall.mall.mapper;

import com.github.pagehelper.Page;
import com.supermall.mall.entity.MallReview;
import com.supermall.mall.vo.MallReviewStatsVO;
import com.supermall.mall.vo.MallReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品评价（mall_review）。
 */
@Mapper
public interface MallReviewMapper {

    void insert(MallReview review);

    MallReview getById(@Param("id") Long id);

    /** 一个订单明细只能评一次（唯一键），这里用于预检与组装 */
    MallReview getByOrderItemId(@Param("orderItemId") Long orderItemId);

    /** 某订单下所有评价（用于「订单评价情况」） */
    List<MallReview> listByOrderId(@Param("orderId") Long orderId);

    /** 某订单明细 id 集合对应的评价（批量，避免 N+1） */
    List<MallReview> listByOrderItemIds(@Param("orderItemIds") List<Long> orderItemIds);

    /** 商品详情页：某商品的评价分页（可选按星级 / 是否有图过滤） */
    Page<MallReviewVO> pageByProduct(@Param("productId") Long productId,
                                     @Param("rating") Integer rating,
                                     @Param("hasImage") Boolean hasImage);

    /** 我的评价分页 */
    Page<MallReviewVO> pageByUser(@Param("userId") Long userId);

    /** 运营后台评价分页 */
    Page<MallReviewVO> pageByAdmin(@Param("productId") Long productId,
                                   @Param("userId") Long userId,
                                   @Param("rating") Integer rating);

    /** 追评：仅当还没追评过（防重复追评） */
    int updateFollow(@Param("id") Long id,
                     @Param("content") String content,
                     @Param("followTime") java.time.LocalDateTime followTime);

    int deleteById(@Param("id") Long id);

    /* ---------- 聚合统计（商品详情 / 重算用） ---------- */

    int countByProduct(@Param("productId") Long productId);

    BigDecimal avgRatingByProduct(@Param("productId") Long productId);

    /** 好评数：rating >= 4 */
    int countGoodByProduct(@Param("productId") Long productId);

    List<MallReviewStatsVO.StarCount> distributionByProduct(@Param("productId") Long productId);
}
