package com.supermall.mall.controller;

import com.supermall.mall.service.MallStatsService;
import com.supermall.mall.vo.MallCategorySalesVO;
import com.supermall.mall.vo.MallSalesTrendVO;
import com.supermall.mall.vo.MallStatsOverviewVO;
import com.supermall.mall.vo.MallTopProductVO;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运营后台 - 销售统计。认证：请求头 mall-admin-token。
 *
 * <p>成交口径：只统计已支付订单（未支付 / 已退款不算）。
 */
@RestController
@RequestMapping("/api/mall/admin/stats")
@Tag(name = "电商-运营后台统计")
@Slf4j
@RequiredArgsConstructor
public class MallAdminStatsController {

    private final MallStatsService mallStatsService;

    @GetMapping("/overview")
    @Operation(summary = "看板总览卡片（成交额 / 订单 / 会员 / 待发货 / 待审退款 / 评价数）")
    public Result<MallStatsOverviewVO> overview() {
        return Result.success(mallStatsService.overview());
    }

    @GetMapping("/sales-trend")
    @Operation(summary = "销售额趋势（折线图）",
            description = "type=day（默认，size 上限 90）或 month（size 上限 24）；无成交的区间补 0")
    public Result<List<MallSalesTrendVO>> salesTrend(@RequestParam(required = false) String type,
                                                     @RequestParam(required = false) Integer size) {
        return Result.success(mallStatsService.salesTrend(type, size));
    }

    @GetMapping("/category-sales")
    @Operation(summary = "品类销售占比（饼图）", description = "按顶级分类归并，percent 已算好")
    public Result<List<MallCategorySalesVO>> categorySales() {
        return Result.success(mallStatsService.categorySales());
    }

    @GetMapping("/top-products")
    @Operation(summary = "热销商品 TOP N（柱状图）", description = "limit 默认 10，上限 50；按销量排序")
    public Result<List<MallTopProductVO>> topProducts(@RequestParam(required = false) Integer limit) {
        return Result.success(mallStatsService.topProducts(limit));
    }
}
