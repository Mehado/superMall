package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallReviewImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评价图片（mall_review_image）。
 */
@Mapper
public interface MallReviewImageMapper {

    void batchInsert(@Param("images") List<MallReviewImage> images);

    /** 批量查一个页面上所有评价的图片（一次查完，避免 N+1） */
    List<MallReviewImage> listByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    void deleteByReviewId(@Param("reviewId") Long reviewId);
}
