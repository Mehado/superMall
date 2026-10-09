package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.entity.MallFootprint;
import com.supermall.mall.entity.MallProduct;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallFootprintMapper;
import com.supermall.mall.mapper.MallProductMapper;
import com.supermall.mall.vo.MallFootprintVO;
import com.supermall.mall.vo.MallProductVO;
import com.github.pagehelper.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 浏览足迹：去重记录、列表组装（批量取卡片避免 N+1）、清空。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallFootprintServiceImpl - 浏览足迹")
class MallFootprintServiceImplTest {

    private static final Long USER = 7L;

    @Mock private MallFootprintMapper mallFootprintMapper;
    @Mock private MallProductMapper mallProductMapper;

    private MallFootprintServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MallFootprintServiceImpl(mallFootprintMapper, mallProductMapper);
        lenient().when(mallProductMapper.getById(anyLong()))
                .thenReturn(MallProduct.builder().id(1L).name("T恤").build());
    }

    @Test
    @DisplayName("记录浏览：交给 upsert（唯一键 + ON DUPLICATE KEY 去重）")
    void recordUsesUpsert() {
        service.record(USER, 1L);
        verify(mallFootprintMapper).upsert(USER, 1L);
    }

    @Test
    @DisplayName("记录浏览：商品不存在 → 拒绝（不写脏数据）")
    void recordUnknownProductRejected() {
        when(mallProductMapper.getById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.record(USER, 999L))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.PRODUCT_NOT_FOUND);
        verify(mallFootprintMapper, never()).upsert(anyLong(), anyLong());
    }

    @Test
    @DisplayName("列表：按足迹顺序组装商品卡片，并带上最近浏览时间与次数")
    void pageAssemblesCards() {
        Page<MallFootprint> page = new Page<>(1, 10);
        page.setTotal(2);
        page.add(MallFootprint.builder().id(1L).userId(USER).productId(10L)
                .visitCount(3).updateTime(LocalDateTime.of(2026, 9, 18, 10, 0)).build());
        page.add(MallFootprint.builder().id(2L).userId(USER).productId(20L)
                .visitCount(1).updateTime(LocalDateTime.of(2026, 9, 18, 9, 0)).build());
        when(mallFootprintMapper.pageByUser(USER)).thenReturn(page);

        when(mallFootprintMapper.listProductCards(List.of(10L, 20L))).thenReturn(List.of(
                MallProductVO.builder().id(10L).name("A").status(1).build(),
                MallProductVO.builder().id(20L).name("B").status(0).build()   // 已下架也要返回
        ));

        var result = service.page(USER, 1, 10);
        List<MallFootprintVO> vos = (List<MallFootprintVO>) result.getRecords();

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(vos).hasSize(2);
        assertThat(vos.get(0).getProduct().getName()).isEqualTo("A");
        assertThat(vos.get(0).getVisitCount()).isEqualTo(3);
        assertThat(vos.get(0).getLastVisitTime()).isEqualTo(LocalDateTime.of(2026, 9, 18, 10, 0));
        // 下架商品仍然返回（前端标灰）
        assertThat(vos.get(1).getProduct().getStatus()).isZero();
    }

    @Test
    @DisplayName("列表：一次批量取卡片（不是 for 里逐个查）")
    void pageLoadsCardsInOneQuery() {
        Page<MallFootprint> page = new Page<>(1, 10);
        page.setTotal(2);
        page.add(MallFootprint.builder().id(1L).userId(USER).productId(10L).visitCount(1).build());
        page.add(MallFootprint.builder().id(2L).userId(USER).productId(20L).visitCount(1).build());
        when(mallFootprintMapper.pageByUser(USER)).thenReturn(page);
        when(mallFootprintMapper.listProductCards(anyListOfLong())).thenReturn(List.of());

        service.page(USER, 1, 10);

        // 只调一次批量查询
        verify(mallFootprintMapper, org.mockito.Mockito.times(1)).listProductCards(anyListOfLong());
        // 商品表的单条查询一次都没用上
        verify(mallProductMapper, never()).getById(anyLong());
    }

    @Test
    @DisplayName("列表：足迹为空时不去查商品卡片")
    void pageEmptySkipsCardQuery() {
        Page<MallFootprint> page = new Page<>(1, 10);
        page.setTotal(0);
        when(mallFootprintMapper.pageByUser(USER)).thenReturn(page);

        var result = service.page(USER, 1, 10);

        assertThat(result.getTotal()).isZero();
        assertThat((List<?>) result.getRecords()).isEmpty();
        verify(mallFootprintMapper, never()).listProductCards(anyListOfLong());
    }

    @Test
    @DisplayName("清空 / 删除单条")
    void clearAndRemoveOne() {
        service.clear(USER);
        verify(mallFootprintMapper).deleteByUser(USER);

        service.removeOne(USER, 10L);
        verify(mallFootprintMapper).deleteByUserAndProduct(USER, 10L);
    }

    private static List<Long> anyListOfLong() {
        return org.mockito.ArgumentMatchers.anyList();
    }
}
