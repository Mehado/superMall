package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价图片（mall_review_image）。
 *
 * <p>{@code type}：1 初评图 / 2 追评图 —— 同一评价的两类图分开取，
 * 前端「初评 3 张 + 追评 1 张」能分别展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallReviewImage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long reviewId;

    private String url;

    /** 1 初评 2 追评 */
    private Integer type;

    private Integer sort;

    private LocalDateTime createTime;
}
