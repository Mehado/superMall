package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallProductPageQueryDTO;
import com.supermall.mall.dto.MallProductSaveDTO;
import com.supermall.mall.entity.MallCategory;
import com.supermall.mall.entity.MallProduct;
import com.supermall.mall.entity.MallSkuSpec;
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
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品保存的核心校验：SKU 必须是规格值的完整笛卡尔积。
 *
 * <p>这是 P1 最容易写错的地方（缺一个组合、多一个组合、规格值对不上、组合重复），
 * 出错的表现是"买家选完规格找不到 SKU"，所以这里逐条卡死。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallProductServiceImpl - 规格 / SKU 矩阵校验")
class MallProductServiceImplTest {

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

        // 叶子分类（可挂商品）
        MallCategory leaf = MallCategory.builder().id(4L).parentId(2L).level(3).name("T恤").status(1).build();
        lenient().when(mallCategoryMapper.getById(anyLong())).thenReturn(leaf);
        lenient().when(mallCategoryMapper.listChildren(anyLong())).thenReturn(List.of());

        // 让 insert 回填自增 id
        AtomicLong seq = new AtomicLong(100);
        lenient().doAnswer(inv -> {
            MallProduct p = inv.getArgument(0);
            p.setId(seq.incrementAndGet());
            return null;
        }).when(mallProductMapper).insert(any(MallProduct.class));
        lenient().doAnswer(inv -> {
            inv.getArgument(0, com.supermall.mall.entity.MallSpec.class).setId(seq.incrementAndGet());
            return null;
        }).when(mallSpecMapper).insert(any(com.supermall.mall.entity.MallSpec.class));
        lenient().doAnswer(inv -> {
            inv.getArgument(0, com.supermall.mall.entity.MallSpecValue.class).setId(seq.incrementAndGet());
            return null;
        }).when(mallSpecValueMapper).insert(any(com.supermall.mall.entity.MallSpecValue.class));
        lenient().doAnswer(inv -> {
            inv.getArgument(0, com.supermall.mall.entity.MallSku.class).setId(seq.incrementAndGet());
            return null;
        }).when(mallSkuMapper).insert(any(com.supermall.mall.entity.MallSku.class));

        // clearProductRelations 要查旧数据，默认没有
        lenient().when(mallSkuMapper.listByProductId(anyLong())).thenReturn(List.of());
        lenient().when(mallSpecMapper.listByProductId(anyLong())).thenReturn(List.of());
    }

    // ---------- 构造器工具 ----------

    private MallProductSaveDTO baseDto() {
        MallProductSaveDTO dto = new MallProductSaveDTO();
        dto.setCategoryId(4L);
        dto.setName("纯棉圆领T恤");
        return dto;
    }

    private MallProductSaveDTO.SpecDTO spec(String name, String... values) {
        MallProductSaveDTO.SpecDTO s = new MallProductSaveDTO.SpecDTO();
        s.setName(name);
        s.setValues(List.of(values));
        return s;
    }

    private MallProductSaveDTO.SkuDTO sku(String price, int stock, String... specValues) {
        MallProductSaveDTO.SkuDTO s = new MallProductSaveDTO.SkuDTO();
        s.setPrice(new BigDecimal(price));
        s.setStock(stock);
        s.setSpecValues(List.of(specValues));
        return s;
    }

    // ---------- 通过 ----------

    @Test
    @DisplayName("2 颜色 × 2 尺码 = 4 个 SKU → 通过，并按规格项顺序建关联")
    void fullMatrixAccepted() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of(spec("颜色", "白", "黑"), spec("尺码", "M", "L")));
        dto.setSkus(List.of(
                sku("79.00", 100, "白", "M"),
                sku("79.00", 120, "白", "L"),
                sku("82.00", 90, "黑", "M"),
                sku("82.00", 80, "黑", "L")));

        assertThatCode(() -> service.save(dto)).doesNotThrowAnyException();

        // 4 个 SKU，每个 2 条规格关联 → 8 条
        verify(mallSkuMapper, times(4)).insert(any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MallSkuSpec>> cap = ArgumentCaptor.forClass(List.class);
        verify(mallSkuSpecMapper).insertBatch(cap.capture());
        assertThat(cap.getValue()).hasSize(8);
        // 保存完必须重算 SPU 上的派生字段
        verify(mallProductMapper).refreshAggregates(anyLong());
    }

    @Test
    @DisplayName("无规格商品：1 个 SKU 且不带规格值 → 通过，不建任何关联")
    void noSpecSingleSkuAccepted() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of());
        dto.setSkus(List.of(sku("39.90", 500)));

        assertThatCode(() -> service.save(dto)).doesNotThrowAnyException();

        verify(mallSkuMapper, times(1)).insert(any());
        verify(mallSkuSpecMapper, never()).insertBatch(any());
    }

    @Test
    @DisplayName("规格值顺序可换（黑/白 与 白/黑 视为不同组合，不冲突）")
    void matrixAcceptsAnyOrderOfSkus() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of(spec("颜色", "白", "黑"), spec("尺码", "M", "L")));
        dto.setSkus(List.of(
                sku("82.00", 1, "黑", "L"),
                sku("79.00", 1, "白", "L"),
                sku("82.00", 1, "黑", "M"),
                sku("79.00", 1, "白", "M")));

        assertThatCode(() -> service.save(dto)).doesNotThrowAnyException();
    }

    // ---------- 矩阵不完整 / 多余 ----------

    @Test
    @DisplayName("SKU 少一个组合 → 拒绝（应等于笛卡尔积）")
    void missingCombinationRejected() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of(spec("颜色", "白", "黑"), spec("尺码", "M", "L")));
        dto.setSkus(List.of(
                sku("79.00", 100, "白", "M"),
                sku("79.00", 120, "白", "L"),
                sku("82.00", 90, "黑", "M")));   // 少了 黑/L

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("SKU 组合数不对")
                .hasMessageContaining("应为 4 个，实际 3 个");
    }

    @Test
    @DisplayName("SKU 多一个组合 → 拒绝")
    void extraCombinationRejected() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of(spec("颜色", "白", "黑")));
        dto.setSkus(List.of(
                sku("79.00", 1, "白"),
                sku("82.00", 1, "黑"),
                sku("83.00", 1, "灰")));   // 灰不在规格值里

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class);
    }

    @Test
    @DisplayName("SKU 的规格值个数与规格项数不一致 → 拒绝")
    void wrongSpecValueCountRejected() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of(spec("颜色", "白", "黑"), spec("尺码", "M", "L")));
        dto.setSkus(List.of(
                sku("79.00", 1, "白", "M"),
                sku("79.00", 1, "白", "L"),
                sku("82.00", 1, "黑", "M"),
                sku("82.00", 1, "黑")));    // 只有 1 个值

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("每个 SKU 需要 2 个规格值");
    }

    @Test
    @DisplayName("SKU 里出现不属于该规格项的值 → 拒绝")
    void unknownSpecValueRejected() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of(spec("颜色", "白", "黑")));
        dto.setSkus(List.of(
                sku("79.00", 1, "白"),
                sku("82.00", 1, "红")));    // 红没定义过

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("不在该商品的规格里");
    }

    @Test
    @DisplayName("两个 SKU 规格组合重复 → 拒绝")
    void duplicateCombinationRejected() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of(spec("颜色", "白", "黑")));
        dto.setSkus(List.of(
                sku("79.00", 1, "白"),
                sku("80.00", 1, "白")));    // 组合重复（数量凑够 2 了）

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("存在重复的规格组合");
    }

    // ---------- 无规格商品的边界 ----------

    @Test
    @DisplayName("无规格商品却给了多个 SKU → 拒绝")
    void noSpecWithMultipleSkusRejected() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of());
        dto.setSkus(List.of(sku("39.90", 1), sku("49.90", 1)));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("无规格商品只能有 1 个 SKU");
    }

    @Test
    @DisplayName("无规格商品却带了规格值 → 拒绝")
    void noSpecButHasSpecValuesRejected() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of());
        dto.setSkus(List.of(sku("39.90", 1, "白")));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("不在该商品的规格里");
    }

    @Test
    @DisplayName("一个 SKU 都没有 → 拒绝")
    void noSkuRejected() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of());
        dto.setSkus(List.of());

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("至少要有一个 SKU");
    }

    // ---------- 规格项本身的问题 ----------

    @Test
    @DisplayName("规格项超过 3 个 → 拒绝")
    void tooManySpecsRejected() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of(spec("a", "1"), spec("b", "1"), spec("c", "1"), spec("d", "1")));
        dto.setSkus(List.of(sku("1.00", 1, "1", "1", "1", "1")));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.SPEC_TOO_MANY);
    }

    @Test
    @DisplayName("规格名重复 → 拒绝")
    void duplicateSpecNameRejected() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of(spec("颜色", "白"), spec("颜色", "黑")));
        dto.setSkus(List.of(sku("1.00", 1, "白", "黑")));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.SPEC_NAME_DUPLICATE);
    }

    @Test
    @DisplayName("同一规格下的规格值重复 → 拒绝")
    void duplicateSpecValueRejected() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of(spec("颜色", "白", "白")));
        dto.setSkus(List.of(sku("1.00", 1, "白"), sku("1.00", 1, "白")));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.SPEC_VALUE_DUPLICATE);
    }

    @Test
    @DisplayName("规格一个值都没有 → 拒绝")
    void emptySpecValuesRejected() {
        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of(spec("颜色")));
        dto.setSkus(List.of(sku("1.00", 1, "白")));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.SPEC_VALUE_EMPTY);
    }

    // ---------- 分类约束 ----------

    @Test
    @DisplayName("商品挂在非叶子分类上 → 拒绝")
    void nonLeafCategoryRejected() {
        when(mallCategoryMapper.listChildren(anyLong()))
                .thenReturn(List.of(MallCategory.builder().id(9L).build()));

        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of());
        dto.setSkus(List.of(sku("1.00", 1)));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.CATEGORY_NOT_LEAF);
    }

    @Test
    @DisplayName("分类被禁用 → 拒绝")
    void disabledCategoryRejected() {
        when(mallCategoryMapper.getById(anyLong()))
                .thenReturn(MallCategory.builder().id(4L).level(3).name("T恤").status(0).build());

        MallProductSaveDTO dto = baseDto();
        dto.setSpecs(List.of());
        dto.setSkus(List.of(sku("1.00", 1)));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("分类已禁用");
    }

    // ---------- 上下架 ----------

    @Test
    @DisplayName("上架：状态只能是 1/0")
    void statusMustBeOneOrZero() {
        when(mallProductMapper.getById(1L)).thenReturn(MallProduct.builder().id(1L).build());

        assertThatThrownBy(() -> service.updateStatus(1L, 2))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("只能是 1（上架）或 0（下架）");
    }

    @Test
    @DisplayName("上架：没有 SKU 不能上架")
    void cannotPutOnSaleWithoutSku() {
        when(mallProductMapper.getById(1L)).thenReturn(MallProduct.builder().id(1L).build());
        when(mallSkuMapper.listByProductId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateStatus(1L, 1))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("没有 SKU，不能上架");
    }

    @Test
    @DisplayName("上架成功后应该调用 updateStatus，而不是全量 update")
    void onSaleUsesDedicatedUpdate() {
        when(mallProductMapper.getById(1L)).thenReturn(MallProduct.builder().id(1L).build());
        when(mallSkuMapper.listByProductId(1L))
                .thenReturn(List.of(com.supermall.mall.entity.MallSku.builder().id(9L).build()));

        service.updateStatus(1L, 1);

        verify(mallProductMapper).updateStatus(1L, 1);
        verify(mallProductMapper, never()).update(any());
    }

    // ---------- 删除 ----------

    @Test
    @DisplayName("上架中的商品不能删")
    void onSaleCannotBeDeleted() {
        when(mallProductMapper.getById(1L)).thenReturn(MallProduct.builder().id(1L).status(1).build());

        assertThatThrownBy(() -> service.remove(1L))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.PRODUCT_ON_SALE_CANNOT_DELETE);
    }

    @Test
    @DisplayName("下架商品可删，且关联数据按顺序清理")
    void offSaleDeleteClearsRelations() {        when(mallProductMapper.getById(1L)).thenReturn(MallProduct.builder().id(1L).status(0).build());
        when(mallSkuMapper.listByProductId(1L))
                .thenReturn(List.of(com.supermall.mall.entity.MallSku.builder().id(7L).build()));
        when(mallSpecMapper.listByProductId(1L))
                .thenReturn(List.of(com.supermall.mall.entity.MallSpec.builder().id(5L).build()));

        service.remove(1L);

        // 先删 sku_spec（依赖 sku），再删 sku；规格值依赖 spec
        verify(mallSkuSpecMapper).deleteByProductId(1L);
        verify(mallSkuMapper).deleteByProductId(1L);
        verify(mallSpecValueMapper).deleteBySpecIds(List.of(5L));
        verify(mallSpecMapper).deleteByProductId(1L);
        verify(mallProductImageMapper).deleteByProductId(1L);
        verify(mallProductMapper).deleteById(1L);
    }
    // ============================================================
    // P2：搜索排序归一化 + 推荐
    // ============================================================

    @Test
    @DisplayName("orderBy 白名单：合法值原样透传")
    void orderByWhitelistPassThrough() {
        for (String v : List.of("price_asc", "price_desc", "sales_desc", "rating_desc", "newest")) {
            MallProductPageQueryDTO q = new MallProductPageQueryDTO();
            q.setOrderBy(v);
            assertThat(q.safeOrderBy()).isEqualTo(v);
        }
    }

    @Test
    @DisplayName("orderBy 非法值 → 回落 default（不把脏值带进 SQL 分支）")
    void orderByIllegalFallsBack() {
        for (String v : List.of("", "id desc; drop table mall_product", "PRICE_ASC", "price", "limit 10")) {
            MallProductPageQueryDTO q = new MallProductPageQueryDTO();
            q.setOrderBy(v);
            assertThat(q.safeOrderBy()).isEqualTo("default");
        }
        MallProductPageQueryDTO nullOne = new MallProductPageQueryDTO();
        assertThat(nullOne.safeOrderBy()).isEqualTo("default");
    }

    @Test
    @DisplayName("page()：排序值在进 Mapper 之前已被归一化")
    void pageNormalizesOrderBy() {
        MallProductPageQueryDTO q = new MallProductPageQueryDTO();
        q.setOrderBy("我乱传的");

        try {
            service.page(q);
        } catch (Exception ignored) {
            /* Mapper 是 mock，PageHelper 可能抛异常，不影响断言 */
        }

        ArgumentCaptor<MallProductPageQueryDTO> cap =
                ArgumentCaptor.forClass(MallProductPageQueryDTO.class);
        verify(mallProductMapper).page(cap.capture());
        assertThat(cap.getValue().getOrderBy()).isEqualTo("default");
    }

    @Test
    @DisplayName("page()：pageSize 上限 100、page 至少 1")
    void pageSizeCapped() {
        MallProductPageQueryDTO q = new MallProductPageQueryDTO();
        q.setPage(0);
        q.setPageSize(9999);
        assertThat(q.safePage()).isEqualTo(1);
        assertThat(q.safePageSize()).isEqualTo(100);

        MallProductPageQueryDTO q2 = new MallProductPageQueryDTO();
        assertThat(q2.safePage()).isEqualTo(1);
        assertThat(q2.safePageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("推荐：type 映射到对应排序，且强制只看上架")
    void recommendMapsTypeToOrder() {
        assertRecommend("hot", "sales_desc");
        assertRecommend("new", "newest");
        assertRecommend("rating", "rating_desc");
        assertRecommend(null, "sales_desc");        // 默认 hot
        assertRecommend("乱传", "sales_desc");       // 未知类型回落 hot
    }

    private void assertRecommend(String type, String expectedOrderBy) {
        org.mockito.Mockito.reset(mallProductMapper);
        when(mallProductMapper.page(any())).thenReturn(new com.github.pagehelper.Page<>());

        try {
            service.recommend(type, 5);
        } catch (Exception ignored) {
            /* PageHelper 在 mock 环境下可能报错，不影响参数断言 */
        }

        ArgumentCaptor<MallProductPageQueryDTO> cap =
                ArgumentCaptor.forClass(MallProductPageQueryDTO.class);
        verify(mallProductMapper).page(cap.capture());
        assertThat(cap.getValue().getOrderBy()).isEqualTo(expectedOrderBy);
        assertThat(cap.getValue().getStatus()).isEqualTo(1);
        assertThat(cap.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("推荐：limit 上限 50，不传默认 10")
    void recommendLimitCapped() {
        MallProductPageQueryDTO big = new MallProductPageQueryDTO();
        big.setPageSize(9999);
        assertThat(big.safePageSize()).isEqualTo(100);

        org.mockito.Mockito.reset(mallProductMapper);
        when(mallProductMapper.page(any())).thenReturn(new com.github.pagehelper.Page<>());
        try {
            service.recommend("hot", 9999);
        } catch (Exception ignored) {
            /* ignore */
        }
        ArgumentCaptor<MallProductPageQueryDTO> cap =
                ArgumentCaptor.forClass(MallProductPageQueryDTO.class);
        verify(mallProductMapper).page(cap.capture());
        assertThat(cap.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("编辑商品重建 SKU 时，按 skuCode 把旧销量带过来（P4 后销量有真实业务含义）")
    void salesCarriedOverOnUpdate() {
        when(mallProductMapper.getById(1L)).thenReturn(MallProduct.builder().id(1L).build());
        // 旧 SKU：TS-W-M 卖过 5 件，TS-B-M 卖过 2 件
        when(mallSkuMapper.listByProductId(1L)).thenReturn(List.of(
                com.supermall.mall.entity.MallSku.builder().id(101L).skuCode("TS-W-M").sales(5).build(),
                com.supermall.mall.entity.MallSku.builder().id(102L).skuCode("TS-B-M").sales(2).build()));
        when(mallSpecMapper.listByProductId(1L)).thenReturn(List.of());

        MallProductSaveDTO dto = baseDto();
        dto.setId(1L);
        dto.setSpecs(List.of(spec("颜色", "白", "黑")));
        MallProductSaveDTO.SkuDTO wm = sku("79.00", 100, "白");
        wm.setSkuCode("TS-W-M");
        MallProductSaveDTO.SkuDTO bm = sku("82.00", 100, "黑");
        bm.setSkuCode("TS-B-M");
        // 颜色规格只有白/黑两个值 → 恰好 2 个 SKU
        dto.setSkus(List.of(wm, bm));

        service.save(dto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.supermall.mall.entity.MallSku> cap =
                ArgumentCaptor.forClass(com.supermall.mall.entity.MallSku.class);
        verify(mallSkuMapper, times(2)).insert(cap.capture());
        assertThat(cap.getAllValues()).extracting(com.supermall.mall.entity.MallSku::getSkuCode)
                .containsExactly("TS-W-M", "TS-B-M");
        assertThat(cap.getAllValues()).extracting(com.supermall.mall.entity.MallSku::getSales)
                .containsExactly(5, 2);
    }
}
