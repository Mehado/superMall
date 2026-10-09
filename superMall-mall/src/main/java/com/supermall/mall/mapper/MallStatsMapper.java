package com.supermall.mall.mapper;

import com.supermall.mall.vo.MallCategorySalesVO;
import com.supermall.mall.vo.MallSalesTrendVO;
import com.supermall.mall.vo.MallStatsOverviewVO;
import com.supermall.mall.vo.MallTopProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营统计（销售看板）。
 *
 * <p>统一口径：**成交额 / 销量只统计 {@code pay_status = 'PAID'} 的订单**
 * （未支付不算；已退款的 pay_status 变成 REFUNDED，也会被排除）。
 */
@Mapper
public interface MallStatsMapper {

    /** 看板卡片：一次查完所有汇总数 */
    MallStatsOverviewVO overview();

    /** 按日销售额（从 begin 起） */
    List<MallSalesTrendVO> salesTrendDay(@Param("begin") LocalDateTime begin);

    /** 按月销售额（从 begin 起） */
    List<MallSalesTrendVO> salesTrendMonth(@Param("begin") LocalDateTime begin);

    /** 品类销售占比（按顶级分类归并） */
    List<MallCategorySalesVO> categorySales();

    /** 热销商品 TOP N */
    List<MallTopProductVO> topProducts(@Param("limit") Integer limit);
}
