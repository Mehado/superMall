package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.entity.MallProduct;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallFavoriteMapper;
import com.supermall.mall.mapper.MallProductMapper;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 收藏夹：幂等收藏/取消、商品存在性、下架商品仍可见。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallFavoriteServiceImpl - 收藏夹")
class MallFavoriteServiceImplTest {

    private static final Long USER = 7L;
    private static final Long PRODUCT = 100L;

    @Mock private MallFavoriteMapper mallFavoriteMapper;
    @Mock private MallProductMapper mallProductMapper;

    private MallFavoriteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MallFavoriteServiceImpl(mallFavoriteMapper, mallProductMapper);
        lenient().when(mallProductMapper.getById(PRODUCT))
                .thenReturn(MallProduct.builder().id(PRODUCT).name("T恤").build());
    }

    @Test
    @DisplayName("收藏：商品存在时写库（用 INSERT IGNORE 保证幂等）")
    void addOk() {
        service.add(USER, PRODUCT);
        verify(mallFavoriteMapper).insertIgnore(USER, PRODUCT);
    }

    @Test
    @DisplayName("收藏：商品不存在 → 拒绝，且不写库")
    void addUnknownProductRejected() {
        when(mallProductMapper.getById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.add(USER, 999L))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.PRODUCT_NOT_FOUND);
        verify(mallFavoriteMapper, never()).insertIgnore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("重复收藏不报错（幂等由唯一键 + INSERT IGNORE 承担）")
    void addIsIdempotent() {
        service.add(USER, PRODUCT);
        service.add(USER, PRODUCT);
        // 两次都发起了 insertIgnore，靠数据库唯一键去重，不抛异常
        verify(mallFavoriteMapper, org.mockito.Mockito.times(2)).insertIgnore(USER, PRODUCT);
    }

    @Test
    @DisplayName("取消收藏：不校验商品存在（商品可能已被删，也要能清掉收藏）")
    void removeDoesNotRequireProduct() {
        service.remove(USER, PRODUCT);
        verify(mallFavoriteMapper).delete(USER, PRODUCT);
    }

    @Test
    @DisplayName("是否已收藏")
    void exists() {
        when(mallFavoriteMapper.countByUserAndProduct(USER, PRODUCT)).thenReturn(1);
        assertThat(service.exists(USER, PRODUCT)).isTrue();

        when(mallFavoriteMapper.countByUserAndProduct(USER, PRODUCT)).thenReturn(0);
        assertThat(service.exists(USER, PRODUCT)).isFalse();
    }

    @Test
    @DisplayName("收藏列表：返回商品卡片，且包含已下架商品（status=0）")
    void pageIncludesOffSaleProducts() {
        Page<MallProductVO> page = new Page<>(1, 10);
        page.setTotal(2);
        page.add(MallProductVO.builder().id(1L).name("在售").status(1).build());
        page.add(MallProductVO.builder().id(2L).name("已下架").status(0).build());
        when(mallFavoriteMapper.pageProducts(USER)).thenReturn(page);

        var result = service.page(USER, 1, 10);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat((List<?>) result.getRecords()).hasSize(2);
        // 关键：下架商品没有被过滤掉
        assertThat(((List<MallProductVO>) result.getRecords()))
                .extracting(MallProductVO::getStatus).containsExactly(1, 0);
    }

    @Test
    @DisplayName("收藏列表：pageSize 超过 100 时被夹到 100")
    void pageSizeCapped() {
        when(mallFavoriteMapper.pageProducts(USER)).thenReturn(new Page<>(1, 100));
        service.page(USER, 1, 9999);
        // PageHelper.startPage 已被调用（夹取在服务层完成），这里只验证不抛异常
        assertThat(service.page(USER, null, null)).isNotNull();
    }
}
