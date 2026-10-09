package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallMemberMapper;
import com.supermall.mall.vo.MallMemberVO;
import com.github.pagehelper.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会员管理：详情不存在、状态合法性、启用/禁用。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallMemberServiceImpl - 会员管理")
class MallMemberServiceImplTest {

    @Mock private MallMemberMapper mallMemberMapper;

    private MallMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MallMemberServiceImpl(mallMemberMapper);
    }

    @Test
    @DisplayName("详情：会员不存在 → 拒绝")
    void detailNotFound() {
        when(mallMemberMapper.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.detail(1L))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("详情：返回统计字段")
    void detailOk() {
        when(mallMemberMapper.getById(1L)).thenReturn(MallMemberVO.builder()
                .id(1L).username("u@qq.com").orderCount(3).totalSpent(new BigDecimal("158.00")).build());
        MallMemberVO vo = service.detail(1L);
        assertThat(vo.getOrderCount()).isEqualTo(3);
        assertThat(vo.getTotalSpent()).isEqualByComparingTo("158.00");
    }

    @Test
    @DisplayName("改状态：非法值 → 拒绝")
    void updateStatusInvalid() {
        assertThatThrownBy(() -> service.updateStatus(1L, 9))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.MEMBER_STATUS_INVALID);
        assertThatThrownBy(() -> service.updateStatus(1L, null))
                .isInstanceOf(MallBusinessException.class);
        verify(mallMemberMapper, never()).updateStatus(anyLong(), anyInt());
    }

    @Test
    @DisplayName("改状态：会员不存在 → 拒绝")
    void updateStatusNotFound() {
        when(mallMemberMapper.countById(1L)).thenReturn(0);
        assertThatThrownBy(() -> service.updateStatus(1L, 0))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("禁用会员：写库成功")
    void disableOk() {
        when(mallMemberMapper.countById(1L)).thenReturn(1);
        service.updateStatus(1L, 0);
        verify(mallMemberMapper).updateStatus(1L, 0);
    }

    @Test
    @DisplayName("分页：返回总数与记录")
    void pageOk() {
        Page<MallMemberVO> page = new Page<>(1, 10);
        page.setTotal(2);
        page.add(MallMemberVO.builder().id(1L).build());
        page.add(MallMemberVO.builder().id(2L).build());
        when(mallMemberMapper.page(eq("张"), eq(1))).thenReturn(page);

        var result = service.page("  张  ", 1, 1, 10);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat((java.util.List<?>) result.getRecords()).hasSize(2);
        verify(mallMemberMapper).page("张", 1);   // keyword 会被 trim
    }
}
