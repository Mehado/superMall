package com.supermall.takeout.service.impl;

import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.ProductDTO;
import com.supermall.takeout.entity.Category;
import com.supermall.takeout.entity.Product;
import com.supermall.takeout.exception.ProductBusinessException;
import com.supermall.takeout.mapper.CategoryMapper;
import com.supermall.takeout.mapper.ProductMapper;
import com.supermall.takeout.mapper.SkuMapper;
import com.supermall.takeout.service.StockService;
import com.supermall.takeout.utils.RedisCacheClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品业态派生 + 随机商品流业态校验的单元测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductServiceImpl - 业态派生与随机流")
class ProductServiceImplTest {

    private static final Long MERCHANT_ID = 1L;

    @Mock
    private ProductMapper productMapper;
    @Mock
    private SkuMapper skuMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private RedisCacheClient cacheClient;
    @Mock
    private StockService stockService;

    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(productMapper, skuMapper, categoryMapper, cacheClient, stockService);
        BaseContext.setCurrentId(MERCHANT_ID);
    }

    @AfterEach
    void tearDown() {
        BaseContext.clear();
    }

    private ProductDTO newProductDto(Long categoryId) {
        ProductDTO dto = new ProductDTO();
        dto.setCategoryId(categoryId);
        dto.setName("洗衣液 2kg");
        dto.setPrice(new BigDecimal("29.90"));
        dto.setStock(50);
        return dto;
    }

    @Test
    @DisplayName("新增商品：业态由分类派生，忽略前端传值")
    void save_derivesBusinessTypeFromCategory() {
        when(categoryMapper.getById(10L, MERCHANT_ID)).thenReturn(
                Category.builder().id(10L).merchantId(MERCHANT_ID).businessType(2).build());

        service.save(newProductDto(10L));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getBusinessType()).isEqualTo(2);
        assertThat(saved.getMerchantId()).isEqualTo(MERCHANT_ID);
        assertThat(saved.getSales()).isZero();
        assertThat(saved.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("新增商品：分类不属于当前商家则拒绝")
    void save_categoryNotOwned_isRejected() {
        when(categoryMapper.getById(anyLong(), anyLong())).thenReturn(null);

        assertThatThrownBy(() -> service.save(newProductDto(99L)))
                .isInstanceOf(ProductBusinessException.class);

        verify(productMapper, never()).insert(any());
    }

    @Test
    @DisplayName("修改商品：业态跟随新分类变化")
    void update_derivesBusinessTypeFromCategory() {
        Product exist = Product.builder().id(5L).merchantId(MERCHANT_ID).categoryId(10L)
                .businessType(2).status(1).build();
        when(productMapper.getById(5L)).thenReturn(exist);
        when(categoryMapper.getById(20L, MERCHANT_ID)).thenReturn(
                Category.builder().id(20L).merchantId(MERCHANT_ID).businessType(1).build());

        ProductDTO dto = newProductDto(20L);
        dto.setId(5L);
        service.update(dto);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).update(captor.capture());
        assertThat(captor.getValue().getBusinessType()).isEqualTo(1);
    }

    @Test
    @DisplayName("随机商品流：非法业态直接拒绝，不查库")
    void randomFeed_invalidBusinessType_isRejected() {
        assertThatThrownBy(() -> service.randomFeed("kw", 9, 6))
                .isInstanceOf(ProductBusinessException.class);

        verify(productMapper, never()).randomOnSale(any(), any(), anyInt());
    }

    @Test
    @DisplayName("随机商品流：业态透传给 Mapper")
    void randomFeed_passesBusinessTypeToMapper() {
        when(productMapper.randomOnSale("kw", 2, 6)).thenReturn(List.of());

        assertThat(service.randomFeed("kw", 2, 6)).isEmpty();

        verify(productMapper).randomOnSale("kw", 2, 6);
    }
}
