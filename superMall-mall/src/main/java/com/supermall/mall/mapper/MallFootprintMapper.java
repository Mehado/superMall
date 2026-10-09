package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallFootprint;
import com.supermall.mall.vo.MallProductVO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 浏览足迹（mall_footprint）。
 */
@Mapper
public interface MallFootprintMapper {

    /**
     * 记录一次浏览（幂等去重）：同一用户同一商品只保留一行，
     * 重复浏览累加 visitCount 并刷新 updateTime。
     */
    int upsert(@Param("userId") Long userId, @Param("productId") Long productId);

    /** 足迹列表（按最近浏览倒序）；返回 product_id + 时间 + 次数，商品卡片由服务层拼 */
    Page<MallFootprint> pageByUser(@Param("userId") Long userId);

    /** 批量取商品卡片（服务层用，避免 N+1） */
    List<MallProductVO> listProductCards(@Param("productIds") List<Long> productIds);

    int deleteByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    int deleteByUser(@Param("userId") Long userId);
}
