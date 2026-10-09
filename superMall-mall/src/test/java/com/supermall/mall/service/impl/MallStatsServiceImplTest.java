package com.supermall.mall.service.impl;

import com.supermall.mall.mapper.MallStatsMapper;
import com.supermall.mall.vo.MallCategorySalesVO;
import com.supermall.mall.vo.MallSalesTrendVO;
import com.supermall.mall.vo.MallStatsOverviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 销售统计：趋势补 0、品类占比、TOP N 上限。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallStatsServiceImpl - 销售统计")
class MallStatsServiceImplTest {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Mock private MallStatsMapper mallStatsMapper;

    private MallStatsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MallStatsServiceImpl(mallStatsMapper);
    }

    private MallSalesTrendVO point(String date, String sales, int orders) {
        return MallSalesTrendVO.builder().date(date).sales(new BigDecimal(sales)).orders(orders).build();
    }

    @Test
    @DisplayName("日趋势：没有成交的日期补 0，保证折线不断档")
    void trendDayFillsGaps() {
        LocalDate today = LocalDate.now();
        String d2 = today.minusDays(2).format(DAY);
        String d0 = today.format(DAY);
        when(mallStatsMapper.salesTrendDay(any())).thenReturn(List.of(
                point(d2, "100.00", 1), point(d0, "50.00", 2)));

        List<MallSalesTrendVO> trend = service.salesTrend("day", 3);

        assertThat(trend).hasSize(3);
        assertThat(trend).extracting(MallSalesTrendVO::getDate)
                .containsExactly(today.minusDays(2).format(DAY), today.minusDays(1).format(DAY), d0);
        assertThat(trend.get(1).getSales()).isEqualByComparingTo("0");
        assertThat(trend.get(1).getOrders()).isZero();
        assertThat(trend.get(2).getSales()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("日趋势：size 缺省 30，上限 90")
    void trendDaySizeClamp() {
        when(mallStatsMapper.salesTrendDay(any())).thenReturn(List.of());
        assertThat(service.salesTrend(null, null)).hasSize(30);
        assertThat(service.salesTrend("day", 999)).hasSize(90);
    }

    @Test
    @DisplayName("月趋势：按 yyyy-MM 补 0，size 上限 24")
    void trendMonth() {
        YearMonth begin = YearMonth.now().minusMonths(2);
        when(mallStatsMapper.salesTrendMonth(any())).thenReturn(List.of(
                point(begin.toString(), "200.00", 1)));

        List<MallSalesTrendVO> trend = service.salesTrend("month", 3);

        assertThat(trend).hasSize(3);
        assertThat(trend.get(0).getDate()).isEqualTo(begin.toString());
        assertThat(trend.get(0).getSales()).isEqualByComparingTo("200.00");
        assertThat(trend.get(1).getSales()).isEqualByComparingTo("0");

        when(mallStatsMapper.salesTrendMonth(any())).thenReturn(List.of());
        assertThat(service.salesTrend("month", 999)).hasSize(24);
    }

    @Test
    @DisplayName("品类占比：服务端算好百分比（30/70 → 30% / 70%）")
    void categoryPercent() {
        when(mallStatsMapper.categorySales()).thenReturn(List.of(
                MallCategorySalesVO.builder().categoryId(1L).categoryName("服饰").amount(new BigDecimal("30.00")).quantity(1).build(),
                MallCategorySalesVO.builder().categoryId(2L).categoryName("食品").amount(new BigDecimal("70.00")).quantity(2).build()));

        List<MallCategorySalesVO> list = service.categorySales();

        assertThat(list.get(0).getPercent()).isEqualByComparingTo("30.00");
        assertThat(list.get(1).getPercent()).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("品类占比：总额为 0 时百分比为 0，不除零")
    void categoryPercentZeroTotal() {
        when(mallStatsMapper.categorySales()).thenReturn(List.of(
                MallCategorySalesVO.builder().categoryId(1L).categoryName("服饰").amount(BigDecimal.ZERO).quantity(0).build()));
        assertThat(service.categorySales().get(0).getPercent()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("TOP N：默认 10，上限 50")
    void topProductsClamp() {
        when(mallStatsMapper.topProducts(any())).thenReturn(List.of());
        service.topProducts(null);
        verify(mallStatsMapper).topProducts(10);
        service.topProducts(999);
        verify(mallStatsMapper).topProducts(50);
    }

    @Test
    @DisplayName("总览：mapper 返回 null 时给全 0（不抛 NPE）")
    void overviewNullSafe() {
        when(mallStatsMapper.overview()).thenReturn(null);
        MallStatsOverviewVO vo = service.overview();
        assertThat(vo.getTotalSales()).isEqualByComparingTo("0");
        assertThat(vo.getMemberCount()).isZero();
    }
}
