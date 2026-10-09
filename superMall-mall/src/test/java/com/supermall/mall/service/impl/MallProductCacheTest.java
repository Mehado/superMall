package com.supermall.mall.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.constant.MallRedisKeyConstant;
import com.supermall.mall.dto.MallProductSaveDTO;
import com.supermall.mall.entity.MallCategory;
import com.supermall.mall.entity.MallProduct;
import com.supermall.mall.entity.MallSku;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallBrandMapper;
import com.supermall.mall.mapper.MallCategoryMapper;
import com.supermall.mall.mapper.MallProductImageMapper;
import com.supermall.mall.mapper.MallProductMapper;
import com.supermall.mall.mapper.MallSkuMapper;
import com.supermall.mall.mapper.MallSkuSpecMapper;
import com.supermall.mall.mapper.MallSpecMapper;
import com.supermall.mall.mapper.MallSpecValueMapper;
import com.supermall.mall.service.MallCategoryService;
import com.supermall.mall.service.MallStockService;
import com.supermall.mall.utils.MallRedisCacheClient;
import com.supermall.mall.vo.MallProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品详情缓存（Cache-Aside 三防）的单元测试。
 *
 * <p>对应外卖 {@code ProductServiceImpl#getDetail} 的同款实现：
 * <ul>
 *   <li><b>防穿透</b>：DB 查不到 → 写 {@code NULL} 标记 60s；命中该标记直接判「商品不存在」且不打 DB；</li>
 *   <li><b>防击穿</b>：未命中先抢 {@code setnx} 锁，抢到才回源；抢不到就短暂等待重试读缓存；</li>
 *   <li><b>防雪崩</b>：写缓存 TTL = 300 + [0,120) 秒随机抖动。</li>
 * </ul>
 * 另外验证「商品有写操作就删缓存」这条 Cache-Aside 的一致性约定。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallProductServiceImpl - 详情缓存三防")
class MallProductCacheTest {

    private static final Long PRODUCT_ID = 66L;
    private static final String KEY = MallRedisKeyConstant.PRODUCT_CACHE_PREFIX + PRODUCT_ID;

    @Mock private MallProductMapper mallProductMapper;
    @Mock private MallProductImageMapper mallProductImageMapper;
    @Mock private MallSpecMapper mallSpecMapper;
    @Mock private MallSpecValueMapper mallSpecValueMapper;
    @Mock private MallSkuMapper mallSkuMapper;
    @Mock private MallSkuSpecMapper mallSkuSpecMapper;
    @Mock private MallCategoryMapper mallCategoryMapper;
    @Mock private MallBrandMapper mallBrandMapper;
    @Mock private MallCategoryService mallCategoryService;
    @Mock private MallStockService mallStockService;
    @Mock private MallRedisCacheClient cacheClient;

    private MallProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MallProductServiceImpl(
                mallProductMapper, mallProductImageMapper, mallSpecMapper, mallSpecValueMapper,
                mallSkuMapper, mallSkuSpecMapper, mallCategoryMapper, mallBrandMapper,
                mallCategoryService, mallStockService, cacheClient);

        // 叶子分类：可挂商品
        MallCategory leaf = MallCategory.builder().id(4L).parentId(2L).level(3).name("T恤").status(1).build();
        lenient().when(mallCategoryMapper.getById(anyLong())).thenReturn(leaf);
        lenient().when(mallCategoryMapper.listChildren(anyLong())).thenReturn(List.of());
    }

    /**
     * 泛型方法 getJson 的 stub 写法：显式指定 TypeReference<MallProductVO>。
     * 参数依次作为「第 1 次 / 第 2 次 …」调用的返回值。
     *
     * <p>注意签名是 {@code (first, rest...)} 而不是单个可变参数 ——
     * 后者写 {@code whenGetJson(null)} 会被当成「传了整个 null 数组」，是 Java 可变参数的经典坑。
     */
    private void whenGetJson(MallProductVO first, MallProductVO... rest) {
        MallProductVO[] restArr = rest == null ? new MallProductVO[0] : rest;
        MallProductVO[] all = new MallProductVO[restArr.length + 1];
        all[0] = first;
        System.arraycopy(restArr, 0, all, 1, restArr.length);
        when(cacheClient.getJson(eq(KEY), ArgumentMatchers.<TypeReference<MallProductVO>>any()))
                .thenReturn(all[0], Arrays.copyOfRange(all, 1, all.length));
    }

    private MallProductVO dbProduct() {
        return MallProductVO.builder()
                .id(PRODUCT_ID)
                .name("纯棉圆领T恤")
                .priceLow(new BigDecimal("79.00"))
                .priceHigh(new BigDecimal("89.00"))
                .stock(100)
                .status(1)
                .build();
    }

    // ---------- 命中 / 未命中 ----------

    @Test
    @DisplayName("命中缓存：直接返回，一次 DB 都不打")
    void cacheHit_returnsWithoutDb() {
        MallProductVO cached = dbProduct();
        whenGetJson(cached);

        assertThat(service.detailCached(PRODUCT_ID)).isSameAs(cached);

        verify(mallProductMapper, never()).getDetailById(any());
        verify(cacheClient, never()).setJson(anyString(), any(), anyLong(), anyLong());
        verify(cacheClient, never()).tryLock(anyString(), anyLong());
    }

    @Test
    @DisplayName("未命中：抢到锁 → 回源组装 → 写缓存（TTL 300 + jitter 120）→ 释放锁")
    void cacheMiss_buildsFromDbAndWritesCache() {
        whenGetJson(null);
        when(cacheClient.tryLock(KEY, 10L)).thenReturn(true);
        MallProductVO db = dbProduct();
        when(mallProductMapper.getDetailById(PRODUCT_ID)).thenReturn(db);

        MallProductVO result = service.detailCached(PRODUCT_ID);

        assertThat(result.getName()).isEqualTo("纯棉圆领T恤");
        assertThat(result.getImages()).isEmpty();
        assertThat(result.getSpecs()).isEmpty();
        assertThat(result.getSkus()).isEmpty();
        verify(cacheClient).setJson(KEY, result, 300L, 120L);
        verify(cacheClient).unlock(KEY);
        verify(cacheClient, never()).setNull(anyString(), anyLong());
    }

    @Test
    @DisplayName("未命中 + 抢到锁 + 双重检查已被别人写入：用缓存值，不查 DB")
    void doubleCheckUsesValueWrittenByOtherThread() {
        MallProductVO written = dbProduct();
        whenGetJson(null, written);
        when(cacheClient.tryLock(KEY, 10L)).thenReturn(true);

        assertThat(service.detailCached(PRODUCT_ID)).isSameAs(written);

        verify(mallProductMapper, never()).getDetailById(any());
        verify(cacheClient).unlock(KEY);
    }

    // ---------- 防穿透 ----------

    @Test
    @DisplayName("防穿透：DB 查不到 → 写 NULL 标记 60s，并抛「商品不存在」")
    void dbMissing_cachesNullMark() {
        whenGetJson(null);
        when(cacheClient.tryLock(KEY, 10L)).thenReturn(true);
        when(mallProductMapper.getDetailById(PRODUCT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.detailCached(PRODUCT_ID))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.PRODUCT_NOT_FOUND);

        verify(cacheClient).setNull(KEY, 60L);
        verify(cacheClient).unlock(KEY);
        verify(cacheClient, never()).setJson(anyString(), any(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("防穿透：命中 NULL 标记 → 直接判不存在，不打 DB、不加锁")
    void nullMarked_skipsDbEntirely() {
        whenGetJson(null);
        when(cacheClient.isNullMarked(KEY)).thenReturn(true);

        assertThatThrownBy(() -> service.detailCached(PRODUCT_ID))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.PRODUCT_NOT_FOUND);

        verify(mallProductMapper, never()).getDetailById(any());
        verify(cacheClient, never()).tryLock(anyString(), anyLong());
        verify(cacheClient, never()).setJson(anyString(), any(), anyLong(), anyLong());
    }

    // ---------- 防击穿 ----------

    @Test
    @DisplayName("防击穿：没抢到锁 → 短暂等待后重试读缓存，命中就用缓存（不回源）")
    void lockNotAcquired_retriesCacheHit() {
        MallProductVO written = dbProduct();
        whenGetJson(null, written);
        when(cacheClient.tryLock(KEY, 10L)).thenReturn(false);

        assertThat(service.detailCached(PRODUCT_ID)).isSameAs(written);

        verify(mallProductMapper, never()).getDetailById(any());
        verify(cacheClient, never()).unlock(anyString());
    }

    @Test
    @DisplayName("防击穿：没抢到锁且缓存仍未写入 → 直接回源（不让用户无限等待）")
    void lockNotAcquired_fallsBackToDb() {
        whenGetJson(null, null);
        when(cacheClient.tryLock(KEY, 10L)).thenReturn(false);
        MallProductVO db = dbProduct();
        when(mallProductMapper.getDetailById(PRODUCT_ID)).thenReturn(db);

        assertThat(service.detailCached(PRODUCT_ID)).isSameAs(db);

        // 回源了但不写缓存（写缓存是持锁者的职责），也不释放别人的锁
        verify(cacheClient, never()).setJson(anyString(), any(), anyLong(), anyLong());
        verify(cacheClient, never()).unlock(anyString());
    }

    // ---------- 后台直连 DB ----------

    @Test
    @DisplayName("后台 detail()：直查 DB，完全不碰缓存（改完立刻看到自己的改动）")
    void adminDetailBypassesCache() {
        MallProductVO db = dbProduct();
        when(mallProductMapper.getDetailById(PRODUCT_ID)).thenReturn(db);

        assertThat(service.detail(PRODUCT_ID)).isSameAs(db);

        verify(cacheClient, never()).getJson(anyString(), any());
        verify(cacheClient, never()).setJson(anyString(), any(), anyLong(), anyLong());
    }

    // ---------- 写操作删缓存 ----------

    @Test
    @DisplayName("修改商品：先写库再删缓存（Cache-Aside）")
    void update_evictsCache() {
        when(mallProductMapper.getById(PRODUCT_ID))
                .thenReturn(MallProduct.builder().id(PRODUCT_ID).status(1).build());

        MallProductSaveDTO dto = new MallProductSaveDTO();
        dto.setId(PRODUCT_ID);
        dto.setCategoryId(4L);
        dto.setName("纯棉圆领T恤（升级款）");
        MallProductSaveDTO.SkuDTO sku = new MallProductSaveDTO.SkuDTO();
        sku.setPrice(new BigDecimal("89.00"));
        sku.setStock(50);
        dto.setSkus(List.of(sku));

        service.save(dto);

        verify(cacheClient).delete(KEY);
    }

    @Test
    @DisplayName("上下架：删缓存（VO 里带 status，不删会让买家看到旧状态）")
    void updateStatus_evictsCache() {
        when(mallProductMapper.getById(PRODUCT_ID))
                .thenReturn(MallProduct.builder().id(PRODUCT_ID).status(0).build());
        when(mallSkuMapper.listByProductId(PRODUCT_ID))
                .thenReturn(List.of(MallSku.builder().id(9L).productId(PRODUCT_ID).status(1).build()));

        service.updateStatus(PRODUCT_ID, 1);

        verify(cacheClient).delete(KEY);
    }

    @Test
    @DisplayName("删除商品：删缓存（否则详情还能从缓存里读出已删商品）")
    void remove_evictsCache() {
        when(mallProductMapper.getById(PRODUCT_ID))
                .thenReturn(MallProduct.builder().id(PRODUCT_ID).status(0).build());

        service.remove(PRODUCT_ID);

        verify(cacheClient).delete(KEY);
    }
}
