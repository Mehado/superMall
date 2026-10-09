package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品评价（mall_review）。
 *
 * <p>粒度是 <b>订单明细</b>：一个 {@code order_item} 只能评一次（唯一键），
 * 所以「这件商品有没有评过」是可判定的，不会重复。
 * 追评是同一行的追加（{@code follow_content}），不改变评分。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallReview implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private String orderNumber;

    private Long orderItemId;

    private Long userId;

    private Long productId;

    private Long skuId;

    /** 1-5 星 */
    private Integer rating;

    private String content;

    /** 1 匿名 0 实名 */
    private Integer anonymous;

    private String followContent;

    /** 非空表示已追评 */
    private LocalDateTime followTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
