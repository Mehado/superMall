package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.entity.MallCartItem;
import com.supermall.mall.entity.MallProduct;
import com.supermall.mall.entity.MallSku;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallCartItemMapper;
import com.supermall.mall.mapper.MallProductMapper;
import com.supermall.mall.mapper.MallSkuMapper;
import com.supermall.mall.vo.MallCartAddVO;
import com.supermall.mall.vo.MallCartItemVO;
import com.supermall.mall.vo.MallCartVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 购物车：价格实时计算、失效商品识别与不计入结算、加购截断、越权防护。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallCartServiceImpl - 购物车")
class MallCartServiceImplTest {

    private static final Long USER = 7L;
    private static final Long SKU = 10L;
    private static final Long PRODUCT = 1L;

    @Mock private MallCartItemMapper cartMapper;
    @Mock private MallSkuMapper skuMapper;
    @Mock private MallProductMapper productMapper;

    private MallCartServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MallCartServiceImpl(cartMapper, skuMapper, productMapper);
        lenient().when(skuMapper.getById(SKU)).thenReturn(MallSku.builder()
                .id(SKU).productId(PRODUCT).price(new BigDecimal("79.00")).stock(10).status(1).build());
        lenient().when(productMapper.getById(PRODUCT)).thenReturn(MallProduct.builder()
                .id(PRODUCT).name("T恤").status(1).build());
    }

    private MallCartItemVO item(long id, int number, int selected, boolean offSale) {
        return MallCartItemVO.builder()
                .id(id).productId(PRODUCT).skuId(SKU).number(number).selected(selected)
                .productName("T恤").productStatus(offSale ? 0 : 1).skuStatus(1)
                .price(new BigDecimal("79.00")).stock(100)
                .build();
    }

    // ---------------- 列表 / 汇总 ----------------

    @Test
    @DisplayName("列表：失效商品不计入结算金额，且返回失效原因")
    void getExcludesInvalidFromAmount() {
        when(cartMapper.listByUser(USER)).thenReturn(List.of(
                item(1L, 2, 1, false),   // 有效 + 选中 → 79*2 = 158
                item(2L, 3, 1, true)));  // 失效（下架），即使 selected=1 也不计

        MallCartVO vo = service.get(USER);

        assertThat(vo.getTotalItems()).isEqualTo(2);
        assertThat(vo.getTotalNumber()).isEqualTo(5);
        assertThat(vo.getValidCount()).isEqualTo(1);
        assertThat(vo.getInvalidCount()).isEqualTo(1);
        assertThat(vo.getSelectedCount()).isEqualTo(1);
        assertThat(vo.getSelectedNumber()).isEqualTo(2);
        assertThat(vo.getSelectedAmount()).isEqualByComparingTo("158.00");
        assertThat(vo.getAllSelected()).isTrue();   // 唯一有效行已选中
        assertThat(vo.getItems().get(1).getValid()).isFalse();
        assertThat(vo.getItems().get(1).getInvalidReason()).isEqualTo("商品已下架");
        // 失效行对外一律显示未选中（即使库里是 1）
        assertThat(vo.getItems().get(1).getSelected()).isEqualTo(0);
        assertThat(vo.getItems().get(1).getSubtotal()).isEqualByComparingTo("237.00");
    }

    @Test
    @DisplayName("列表：库存不足的行也是失效，给出剩余件数")
    void getMarksStockShortageInvalid() {
        MallCartItemVO shortage = item(1L, 5, 1, false);
        shortage.setStock(3);
        when(cartMapper.listByUser(USER)).thenReturn(List.of(shortage));

        MallCartVO vo = service.get(USER);
        assertThat(vo.getItems().get(0).getValid()).isFalse();
        assertThat(vo.getItems().get(0).getInvalidReason()).contains("仅剩 3 件");
        assertThat(vo.getSelectedAmount()).isEqualByComparingTo("0");
        assertThat(vo.getAllSelected()).isFalse();
    }

    @Test
    @DisplayName("列表：没有有效行时 allSelected=false（避免空车显示勾选）")
    void allSelectedFalseWhenNoValidItem() {
        when(cartMapper.listByUser(USER)).thenReturn(List.of(item(1L, 1, 0, true)));
        assertThat(service.get(USER).getAllSelected()).isFalse();
    }

    // ---------------- 加购 ----------------

    @Test
    @DisplayName("加购：累加并返回最终数量")
    void addAccumulates() {
        when(cartMapper.getByUserAndSku(USER, SKU)).thenReturn(
                MallCartItem.builder().id(1L).number(2).build(),
                MallCartItem.builder().id(1L).number(5).build());

        MallCartAddVO result = service.add(USER, SKU, 3);

        verify(cartMapper).upsertAdd(USER, PRODUCT, SKU, 3, 10);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getNumber()).isEqualTo(5);
    }

    @Test
    @DisplayName("加购：数量会被库存上限截断（SQL 里 LEAST，返回最终值）")
    void addClampedByStock() {
        when(cartMapper.getByUserAndSku(USER, SKU)).thenReturn(
                MallCartItem.builder().id(1L).number(8).build(),
                MallCartItem.builder().id(1L).number(10).build());
        // 库存 10，已有 8，加 5 → 截断成 10
        MallCartAddVO result = service.add(USER, SKU, 5);
        assertThat(result.getNumber()).isEqualTo(10);
    }

    @Test
    @DisplayName("加购：商品下架 / 规格停用 / 售罄 全部拒绝")
    void addRejectsUnavailable() {
        when(productMapper.getById(PRODUCT)).thenReturn(MallProduct.builder().id(PRODUCT).status(0).build());
        assertThatThrownBy(() -> service.add(USER, SKU, 1))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.CART_PRODUCT_OFF_SALE);

        when(productMapper.getById(PRODUCT)).thenReturn(MallProduct.builder().id(PRODUCT).status(1).build());
        when(skuMapper.getById(SKU)).thenReturn(MallSku.builder().id(SKU).productId(PRODUCT).status(0).stock(5).build());
        assertThatThrownBy(() -> service.add(USER, SKU, 1))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.CART_SKU_DISABLED);

        when(skuMapper.getById(SKU)).thenReturn(MallSku.builder().id(SKU).productId(PRODUCT).status(1).stock(0).build());
        assertThatThrownBy(() -> service.add(USER, SKU, 1))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.CART_OUT_OF_STOCK);

        when(skuMapper.getById(999L)).thenReturn(null);
        assertThatThrownBy(() -> service.add(USER, 999L, 1))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.CART_SKU_NOT_FOUND);

        verify(cartMapper, never()).upsertAdd(anyLong(), anyLong(), anyLong(), anyInt(), anyInt());
    }

    // ---------------- 改数量 / 勾选 ----------------

    @Test
    @DisplayName("改数量：超过库存被拒，正常值放行")
    void updateNumberChecksStock() {
        when(cartMapper.getById(1L)).thenReturn(MallCartItem.builder().id(1L).userId(USER).skuId(SKU).number(1).build());

        assertThatThrownBy(() -> service.updateNumber(USER, 1L, 11))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining(MallMessageConstant.CART_STOCK_NOT_ENOUGH);

        service.updateNumber(USER, 1L, 5);
        verify(cartMapper).updateNumber(1L, USER, 5);
    }

    @Test
    @DisplayName("改数量：改别人的购物车行 → 拒绝")
    void updateNumberRejectsOthers() {
        when(cartMapper.getById(1L)).thenReturn(MallCartItem.builder().id(1L).userId(999L).skuId(SKU).build());
        assertThatThrownBy(() -> service.updateNumber(USER, 1L, 2))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.CART_ITEM_NOT_FOUND);
        verify(cartMapper, never()).updateNumber(anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("勾选：失效商品不能选中")
    void selectInvalidRejected() {
        when(cartMapper.getById(1L)).thenReturn(MallCartItem.builder().id(1L).userId(USER).productId(PRODUCT).skuId(SKU).number(1).build());
        when(productMapper.getById(PRODUCT)).thenReturn(MallProduct.builder().id(PRODUCT).status(0).build());

        assertThatThrownBy(() -> service.updateSelected(USER, 1L, 1))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining(MallMessageConstant.CART_ITEM_INVALID);
        verify(cartMapper, never()).updateSelected(anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("全选：只勾有效行；全不选清所有")
    void selectAllOnlyValid() {
        service.updateAllSelected(USER, 1);
        verify(cartMapper).updateSelectedValid(USER, 1);
        verify(cartMapper, never()).updateSelectedAll(eq(USER), anyInt());

        service.updateAllSelected(USER, 0);
        verify(cartMapper).updateSelectedAll(USER, 0);
    }

    @Test
    @DisplayName("选中状态只能是 0/1")
    void selectedValueValidated() {
        assertThatThrownBy(() -> service.updateAllSelected(USER, 2))
                .isInstanceOf(MallBusinessException.class);
        assertThatThrownBy(() -> service.updateSelected(USER, 1L, null))
                .isInstanceOf(MallBusinessException.class);
    }

    // ---------------- 清理 ----------------

    @Test
    @DisplayName("清理：scope=invalid 只删失效；默认 all 清空；非法 scope 被拒")
    void clearScopes() {
        service.clear(USER, "invalid");
        verify(cartMapper).deleteInvalid(USER);

        service.clear(USER, null);
        verify(cartMapper).deleteAll(USER);

        assertThatThrownBy(() -> service.clear(USER, "weird"))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.CART_SCOPE_INVALID);
    }

    @Test
    @DisplayName("删除单行是幂等的（不抛异常）")
    void removeIdempotent() {
        service.remove(USER, 123L);
        verify(cartMapper).deleteById(123L, USER);
    }
}
