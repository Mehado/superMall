package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallRedisKeyConstant;
import com.supermall.mall.entity.MallOrderItem;
import com.supermall.mall.entity.MallSku;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallProductMapper;
import com.supermall.mall.mapper.MallSkuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库存服务：预热 / 同步 / 预扣 / 聚合去重 / 销量调整。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallStockServiceImpl - 库存")
class MallStockServiceImplTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private MallSkuMapper skuMapper;
    @Mock private MallProductMapper productMapper;
    @Mock private DefaultRedisScript<Long> deductScript;
    @Mock private DefaultRedisScript<Long> releaseScript;

    private MallStockServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MallStockServiceImpl(redisTemplate, skuMapper, productMapper, deductScript, releaseScript);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private MallOrderItem item(long skuId, long productId, int number) {
        return MallOrderItem.builder().skuId(skuId).productId(productId).number(number)
                .price(new BigDecimal("79.00")).build();
    }

    @Test
    @DisplayName("预热：每个 SKU 写一条 mall:stock:sku:{id}")
    void warmUpAll() {
        when(skuMapper.listAll()).thenReturn(List.of(
                MallSku.builder().id(1L).stock(100).build(),
                MallSku.builder().id(2L).stock(50).build()));

        service.warmUpAll();

        verify(valueOps).set(MallRedisKeyConstant.SKU_STOCK_PREFIX + 1L, "100");
        verify(valueOps).set(MallRedisKeyConstant.SKU_STOCK_PREFIX + 2L, "50");
    }

    @Test
    @DisplayName("商品保存后按新 SKU id 重新同步库存")
    void syncProductSkuStock() {
        when(skuMapper.listByProductId(1L)).thenReturn(List.of(
                MallSku.builder().id(10L).stock(3).build(),
                MallSku.builder().id(11L).stock(4).build()));

        service.syncProductSkuStock(1L);

        verify(valueOps).set(MallRedisKeyConstant.SKU_STOCK_PREFIX + 10L, "3");
        verify(valueOps).set(MallRedisKeyConstant.SKU_STOCK_PREFIX + 11L, "4");
    }

    @Test
    @DisplayName("Redis 预扣成功：按 SKU 聚合后逐个执行脚本")
    void deductRedisOk() {
        when(redisTemplate.execute(any(), anyList(), any())).thenReturn(1L);

        service.deductRedis(List.of(item(1L, 1L, 2), item(1L, 1L, 3), item(2L, 2L, 1)));

        // 同一 SKU 聚合成一次扣减 5
        verify(redisTemplate, times(2)).execute(any(), anyList(), any());
    }

    @Test
    @DisplayName("Redis 预扣库存不足：抛异常并回补已扣部分")
    void deductRedisNotEnough() {
        when(redisTemplate.execute(any(), anyList(), any()))
                .thenReturn(1L)    // 第一个成功
                .thenReturn(-1L);  // 第二个不足
        var items = List.of(item(1L, 1L, 2), item(2L, 2L, 999));

        assertThatThrownBy(() -> service.deductRedis(items))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage("库存不足");
        // 回补第一个（release 脚本再执行一次）
        verify(redisTemplate, times(3)).execute(any(), anyList(), any());
    }

    @Test
    @DisplayName("Redis 预扣未预热：抛异常")
    void deductRedisNotWarmed() {
        when(redisTemplate.execute(any(), anyList(), any())).thenReturn(-2L);
        assertThatThrownBy(() -> service.deductRedis(List.of(item(1L, 1L, 1))))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage("库存未预热，请稍后重试");
    }

    @Test
    @DisplayName("重算 SPU 聚合：同一商品只刷一次")
    void refreshAggregatesDedup() {
        service.refreshAggregates(List.of(item(1L, 7L, 1), item(2L, 7L, 1), item(3L, 8L, 1)));
        verify(productMapper, times(1)).refreshAggregates(7L);
        verify(productMapper, times(1)).refreshAggregates(8L);
    }

    @Test
    @DisplayName("销量调整：SKU 与 SPU 按数量累加/扣回")
    void adjustSales() {
        service.adjustSales(List.of(item(1L, 7L, 2)), 1);
        verify(skuMapper).incrementSales(1L, 2);
        verify(productMapper).incrementSales(7L, 2);

        service.adjustSales(List.of(item(1L, 7L, 2)), -1);
        verify(skuMapper).incrementSales(1L, -2);
        verify(productMapper).incrementSales(7L, -2);
    }

    @Test
    @DisplayName("空明细：不触发任何 DB 操作")
    void emptyItems() {
        service.refreshAggregates(List.of());
        service.adjustSales(List.of(), 1);
        service.releaseRedis(List.of());
        verify(productMapper, never()).refreshAggregates(any());
        verify(skuMapper, never()).incrementSales(any(), anyInt());
    }
}
