package com.supermall.mall.service;

import com.supermall.mall.vo.MallCategorySalesVO;
import com.supermall.mall.vo.MallSalesTrendVO;
import com.supermall.mall.vo.MallStatsOverviewVO;
import com.supermall.mall.vo.MallTopProductVO;

import java.util.List;

/**
 * 运营后台 - 销售统计。
 *
 * <p>成交口径：**只统计 {@code pay_status = 'PAID'} 的订单**（未支付、已退款都不算）。
 */
public interface MallStatsService {

    /** 看板总览卡片 */
    MallStatsOverviewVO overview();

    /**
     * 销售额趋势（折线图）。
     *
     * @param type {@code day}（默认）或 {@code month}
     * @param size 返回多少个点（默认 30，日上限 90、月上限 24）；无成交的区间补 0
     */
    List<MallSalesTrendVO> salesTrend(String type, Integer size);

    /** 品类销售占比（饼图） */
    List<MallCategorySalesVO> categorySales();

    /** 热销商品 TOP N（柱状图） */
    List<MallTopProductVO> topProducts(Integer limit);
}
