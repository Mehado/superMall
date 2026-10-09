package com.supermall.mall.service.impl;

import com.supermall.mall.mapper.MallStatsMapper;
import com.supermall.mall.service.MallStatsService;
import com.supermall.mall.vo.MallCategorySalesVO;
import com.supermall.mall.vo.MallSalesTrendVO;
import com.supermall.mall.vo.MallStatsOverviewVO;
import com.supermall.mall.vo.MallTopProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售统计实现。
 *
 * <p>趋势接口**会把没有成交的日期补 0**，让前端折线图不断档
 * （SQL 只 GROUP BY 有数据的分组，补洞在服务层做，比构造日期维表简单）。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallStatsServiceImpl implements MallStatsService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 90;
    private static final int DEFAULT_MONTHS = 12;
    private static final int MAX_MONTHS = 24;

    private final MallStatsMapper mallStatsMapper;

    @Override
    public MallStatsOverviewVO overview() {
        MallStatsOverviewVO vo = mallStatsMapper.overview();
        return vo == null ? emptyOverview() : vo;
    }

    @Override
    public List<MallSalesTrendVO> salesTrend(String type, Integer size) {
        if ("month".equalsIgnoreCase(type)) {
            int n = clamp(size, DEFAULT_MONTHS, MAX_MONTHS);
            YearMonth begin = YearMonth.now().minusMonths(n - 1L);
            Map<String, MallSalesTrendVO> existing = index(
                    mallStatsMapper.salesTrendMonth(begin.atDay(1).atStartOfDay()));
            List<MallSalesTrendVO> result = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                String key = begin.plusMonths(i).toString();   // yyyy-MM
                result.add(existing.getOrDefault(key, zero(key)));
            }
            return result;
        }

        int n = clamp(size, DEFAULT_DAYS, MAX_DAYS);
        LocalDate begin = LocalDate.now().minusDays(n - 1L);
        Map<String, MallSalesTrendVO> existing = index(
                mallStatsMapper.salesTrendDay(begin.atStartOfDay()));
        List<MallSalesTrendVO> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String key = begin.plusDays(i).format(DAY);
            result.add(existing.getOrDefault(key, zero(key)));
        }
        return result;
    }

    @Override
    public List<MallCategorySalesVO> categorySales() {
        List<MallCategorySalesVO> list = mallStatsMapper.categorySales();
        BigDecimal total = BigDecimal.ZERO;
        for (MallCategorySalesVO v : list) {
            total = total.add(v.getAmount() == null ? BigDecimal.ZERO : v.getAmount());
        }
        for (MallCategorySalesVO v : list) {
            BigDecimal amount = v.getAmount() == null ? BigDecimal.ZERO : v.getAmount();
            v.setPercent(total.signum() == 0
                    ? BigDecimal.ZERO
                    : amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP));
        }
        return list;
    }

    @Override
    public List<MallTopProductVO> topProducts(Integer limit) {
        int size = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
        return mallStatsMapper.topProducts(size);
    }

    // ============================================================
    // 私有
    // ============================================================

    private Map<String, MallSalesTrendVO> index(List<MallSalesTrendVO> list) {
        Map<String, MallSalesTrendVO> map = new HashMap<>();
        if (list != null) {
            for (MallSalesTrendVO v : list) {
                map.put(v.getDate(), v);
            }
        }
        return map;
    }

    private MallSalesTrendVO zero(String date) {
        return MallSalesTrendVO.builder()
                .date(date)
                .sales(BigDecimal.ZERO)
                .orders(0)
                .build();
    }

    private int clamp(Integer size, int defaultValue, int max) {
        if (size == null || size < 1) {
            return defaultValue;
        }
        return Math.min(size, max);
    }

    private MallStatsOverviewVO emptyOverview() {
        return MallStatsOverviewVO.builder()
                .totalSales(BigDecimal.ZERO).paidOrders(0).totalOrders(0)
                .todaySales(BigDecimal.ZERO).todayOrders(0)
                .memberCount(0).onSaleProducts(0).pendingDelivery(0).refunding(0).reviewCount(0)
                .build();
    }
}
