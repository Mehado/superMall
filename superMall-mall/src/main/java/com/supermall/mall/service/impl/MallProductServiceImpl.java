package com.supermall.mall.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.constant.MallRedisKeyConstant;
import com.supermall.mall.dto.MallProductPageQueryDTO;
import com.supermall.mall.dto.MallProductSaveDTO;
import com.supermall.mall.entity.MallBrand;
import com.supermall.mall.entity.MallCategory;
import com.supermall.mall.entity.MallProduct;
import com.supermall.mall.entity.MallProductImage;
import com.supermall.mall.entity.MallSku;
import com.supermall.mall.entity.MallSkuSpec;
import com.supermall.mall.entity.MallSpec;
import com.supermall.mall.entity.MallSpecValue;
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
import com.supermall.mall.service.MallProductService;
import com.supermall.mall.service.MallStockService;
import com.supermall.mall.utils.MallRedisCacheClient;
import com.supermall.mall.vo.MallProductVO;
import com.supermall.mall.vo.MallSkuVO;
import com.supermall.mall.vo.MallSpecVO;
import com.supermall.result.PageResult;
import com.supermall.utils.RichTextSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品 SPU + 规格 + SKU。
 *
 * <p>核心是 {@link #save}：一次请求里要把 SPU、多图、规格项、规格值、SKU、SKU↔规格值关联
 * 全部写对，并且保证「SKU 组合数 = 各规格值数量的笛卡尔积」。
 *
 * <p>详情缓存（{@link #detailCached}）与外卖同款三防：
 * 防穿透（空值缓存短 TTL）、防击穿（setnx 互斥锁 + 双重检查）、防雪崩（随机 TTL）。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallProductServiceImpl implements MallProductService {

    private static final int STATUS_ON_SALE = 1;
    private static final int STATUS_OFF_SALE = 0;
    private static final int MAX_SPEC_COUNT = 3;
    private static final int MAX_IMAGE_COUNT = 10;

    /* ---------- 详情缓存参数（与外卖 ProductServiceImpl 保持一致） ---------- */

    /** 缓存 key 前缀：mall:product:detail:{id} */
    private static final String CACHE_KEY_PREFIX = MallRedisKeyConstant.PRODUCT_CACHE_PREFIX;
    /** 基础 TTL 5 分钟 */
    private static final long CACHE_BASE_SECONDS = 300;
    /** 随机抖动：最终 TTL = 300 + [0,120) 秒 —— 防雪崩 */
    private static final long CACHE_JITTER_SECONDS = 120;
    /** 空值缓存 60 秒 —— 防穿透 */
    private static final long NULL_CACHE_SECONDS = 60;
    /** 互斥锁 10 秒（够一次回源；进程挂了也能自动释放） */
    private static final long CACHE_LOCK_SECONDS = 10;

    private final MallProductMapper mallProductMapper;
    private final MallProductImageMapper mallProductImageMapper;
    private final MallSpecMapper mallSpecMapper;
    private final MallSpecValueMapper mallSpecValueMapper;
    private final MallSkuMapper mallSkuMapper;
    private final MallSkuSpecMapper mallSkuSpecMapper;
    private final MallCategoryMapper mallCategoryMapper;
    private final MallBrandMapper mallBrandMapper;
    private final MallCategoryService mallCategoryService;
    private final MallStockService mallStockService;
    private final MallRedisCacheClient cacheClient;

    // ============================================================
    // 查询
    // ============================================================

    @Override
    public PageResult page(MallProductPageQueryDTO query) {
        // 传了父分类就展开成「自己 + 所有后代」，这样点「服饰鞋包」能查到 T恤/衬衫/运动鞋 下的商品
        if (query.getCategoryId() != null) {
            query.setCategoryIds(mallCategoryService.selfAndDescendantIds(query.getCategoryId()));
        }
        // 排序归一化：只认白名单，非法值回落 default。
        // 这样 XML 里的 <choose> 分支永远只看到固定枚举，不会把脏值带进 SQL。
        query.setOrderBy(query.safeOrderBy());

        PageHelper.startPage(query.safePage(), query.safePageSize());
        Page<MallProductVO> page = mallProductMapper.page(query);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public List<MallProductVO> recommend(String type, Integer limit) {
        int size = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
        MallProductPageQueryDTO query = new MallProductPageQueryDTO();
        query.setPage(1);
        query.setPageSize(size);
        query.setStatus(STATUS_ON_SALE);
        query.setOrderBy(switch (type == null ? "hot" : type) {
            case "new" -> "newest";
            case "rating" -> "rating_desc";
            default -> "sales_desc";
        });

        PageHelper.startPage(1, size);
        Page<MallProductVO> page = mallProductMapper.page(query);
        return page.getResult();
    }

    /** 后台编辑用：直查 DB，不加缓存（改完要立刻看到自己的改动） */
    @Override
    public MallProductVO detail(Long id) {
        return buildDetail(id);
    }

    /**
     * 公开目录用：Cache-Aside + 三防（与外卖 ProductServiceImpl#getDetail 同款）。
     *
     * <pre>
     * 读缓存 → 命中空值标记则直接判不存在（防穿透）
     *        → 未命中：setnx 互斥锁 + 双重检查，只放一个请求回源（防击穿）
     *        → 回源结果写缓存，TTL = 300 + [0,120) 秒（防雪崩）
     * </pre>
     */
    @Override
    public MallProductVO detailCached(Long id) {
        String key = CACHE_KEY_PREFIX + id;
        TypeReference<MallProductVO> type = new TypeReference<>() {
        };

        // 1) 先查缓存
        MallProductVO cached = cacheClient.getJson(key, type);
        if (cached != null) {
            return cached;
        }
        // 2) 命中空值标记：商品已确认不存在，直接挡住，不再打 DB —— 防穿透
        if (cacheClient.isNullMarked(key)) {
            throw new MallBusinessException(MallMessageConstant.PRODUCT_NOT_FOUND);
        }
        // 3) 未命中：用互斥锁防击穿，只让一个请求回源
        if (!cacheClient.tryLock(key, CACHE_LOCK_SECONDS)) {
            // 没抢到锁：短暂等待后重试读缓存，仍没有则回源（避免无限等待）
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            MallProductVO retry = cacheClient.getJson(key, type);
            if (retry != null) {
                return retry;
            }
            return buildDetail(id);
        }
        try {
            // 双重检查：可能已被其他线程写入
            MallProductVO doubleCheck = cacheClient.getJson(key, type);
            if (doubleCheck != null) {
                return doubleCheck;
            }
            MallProductVO vo = buildDetail(id);
            // 4) 随机 TTL 写缓存 —— 防雪崩
            cacheClient.setJson(key, vo, CACHE_BASE_SECONDS, CACHE_JITTER_SECONDS);
            return vo;
        } catch (MallBusinessException e) {
            // 商品不存在：缓存空值 60s，避免同一个无效 id 被反复打到 DB
            if (MallMessageConstant.PRODUCT_NOT_FOUND.equals(e.getMessage())) {
                cacheClient.setNull(key, NULL_CACHE_SECONDS);
            }
            throw e;
        } finally {
            cacheClient.unlock(key);
        }
    }

    /** 真正回源组装详情（多图 + 规格项/规格值 + SKU），不含任何缓存逻辑 */
    private MallProductVO buildDetail(Long id) {
        MallProductVO vo = mallProductMapper.getDetailById(id);
        if (vo == null) {
            throw new MallBusinessException(MallMessageConstant.PRODUCT_NOT_FOUND);
        }

        // 多图
        List<String> images = mallProductImageMapper.listByProductId(id).stream()
                .map(MallProductImage::getUrl)
                .toList();
        vo.setImages(images);

        // 规格项 + 规格值
        List<MallSpec> specs = mallSpecMapper.listByProductId(id);
        List<MallSpecVO> specVOs = new ArrayList<>();
        Map<Long, String> specNameById = new HashMap<>();
        Map<Long, String> valueById = new HashMap<>();
        if (!specs.isEmpty()) {
            for (MallSpec s : specs) {
                specNameById.put(s.getId(), s.getName());
            }
            List<Long> specIds = specs.stream().map(MallSpec::getId).toList();
            List<MallSpecValue> values = mallSpecValueMapper.listBySpecIds(specIds);

            // spec -> values（保持 listBySpecIds 的顺序：spec_id asc, sort asc）
            Map<Long, List<String>> valuesBySpec = new LinkedHashMap<>();
            for (MallSpecValue v : values) {
                valueById.put(v.getId(), v.getValue());
                valuesBySpec.computeIfAbsent(v.getSpecId(), k -> new ArrayList<>()).add(v.getValue());
            }
            for (MallSpec s : specs) {
                specVOs.add(MallSpecVO.builder()
                        .name(s.getName())
                        .values(valuesBySpec.getOrDefault(s.getId(), new ArrayList<>()))
                        .build());
            }
        }
        vo.setSpecs(specVOs);

        // SKU + 每个 SKU 的规格值（按规格项顺序）
        List<MallSku> skus = mallSkuMapper.listByProductId(id);
        vo.setSkus(buildSkuVOs(skus, specs, valueById));

        return vo;
    }

    /** 组装 SKU 列表：把 sku_spec 关联翻译成「按规格项顺序的规格值字符串」 */
    private List<MallSkuVO> buildSkuVOs(List<MallSku> skus,
                                        List<MallSpec> specs,
                                        Map<Long, String> valueById) {
        List<MallSkuVO> result = new ArrayList<>();
        if (skus.isEmpty()) {
            return result;
        }
        List<Long> skuIds = skus.stream().map(MallSku::getId).toList();
        List<MallSkuSpec> links = mallSkuSpecMapper.listBySkuIds(skuIds);

        // skuId -> (specId -> valueId)
        Map<Long, Map<Long, Long>> bySku = new HashMap<>();
        for (MallSkuSpec l : links) {
            bySku.computeIfAbsent(l.getSkuId(), k -> new HashMap<>())
                    .put(l.getSpecId(), l.getSpecValueId());
        }

        for (MallSku s : skus) {
            Map<Long, Long> picked = bySku.getOrDefault(s.getId(), Map.of());
            List<String> values = new ArrayList<>();
            for (MallSpec spec : specs) {
                Long valueId = picked.get(spec.getId());
                values.add(valueId == null ? "" : valueById.getOrDefault(valueId, ""));
            }
            result.add(MallSkuVO.builder()
                    .id(s.getId())
                    .skuCode(s.getSkuCode())
                    .price(s.getPrice())
                    .stock(s.getStock())
                    .sales(s.getSales())
                    .image(s.getImage())
                    .status(s.getStatus())
                    .specValues(values)
                    .build());
        }
        return result;
    }

    // ============================================================
    // 保存
    // ============================================================

    @Override
    @Transactional
    public Long save(MallProductSaveDTO dto) {
        // 1) 分类：必须存在、启用、且是叶子（商品挂中间分类会让分类树筛选出现双份）
        MallCategory category = requireLeafCategory(dto.getCategoryId());

        // 2) 品牌（可选）
        if (dto.getBrandId() != null) {
            MallBrand brand = mallBrandMapper.getById(dto.getBrandId());
            if (brand == null) {
                throw new MallBusinessException(MallMessageConstant.BRAND_NOT_FOUND);
            }
        }

        // 3) 规格 + SKU 矩阵校验
        List<MallProductSaveDTO.SpecDTO> specs = dto.getSpecs() == null ? List.of() : dto.getSpecs();
        List<MallProductSaveDTO.SkuDTO> skus = dto.getSkus() == null ? List.of() : dto.getSkus();
        List<List<String>> specValues = normalizeSpecs(specs);
        validateSkuMatrix(specValues, skus);

        // 4) 图片
        List<String> images = dto.getImages() == null ? List.of() : dto.getImages();
        if (images.size() > MAX_IMAGE_COUNT) {
            throw new MallBusinessException(MallMessageConstant.PRODUCT_IMAGE_TOO_MANY);
        }
        images = images.stream().filter(this::hasText).map(String::trim).toList();
        String mainImage = hasText(dto.getMainImage())
                ? dto.getMainImage().trim()
                : (images.isEmpty() ? null : images.get(0));

        // 5) SPU：新增或更新
        Long productId;
        LocalDateTime now = LocalDateTime.now();
        if (dto.getId() == null) {
            MallProduct p = MallProduct.builder()
                    .categoryId(category.getId())
                    .brandId(dto.getBrandId())
                    .name(dto.getName().trim())
                    .subtitle(dto.getSubtitle())
                    .mainImage(mainImage)
                    // 富文本入库前做 XSS 清洗（白名单标签，见 RichTextSanitizer）
                    .detail(RichTextSanitizer.richText(dto.getDetail()))
                    .priceLow(BigDecimal.ZERO)
                    .priceHigh(BigDecimal.ZERO)
                    .stock(0)
                    .sales(0)
                    .rating(new BigDecimal("5.00"))
                    .status(dto.getStatus() == null ? STATUS_OFF_SALE : dto.getStatus())
                    .sort(dto.getSort() == null ? 0 : dto.getSort())
                    .createTime(now)
                    .updateTime(now)
                    .build();
            mallProductMapper.insert(p);
            productId = p.getId();
        } else {
            requireProduct(dto.getId());
            productId = dto.getId();
            mallProductMapper.update(MallProduct.builder()
                    .id(productId)
                    .categoryId(category.getId())
                    .brandId(dto.getBrandId())
                    .name(dto.getName().trim())
                    .subtitle(dto.getSubtitle())
                    .mainImage(mainImage)
                    .detail(RichTextSanitizer.richText(dto.getDetail()))
                    .status(dto.getStatus() == null ? STATUS_OFF_SALE : dto.getStatus())
                    .sort(dto.getSort() == null ? 0 : dto.getSort())
                    .updateTime(now)
                    .build());
        }

        // 6) 重建 图片 / 规格 / 规格值 / SKU / 关联
        //    先记下旧 SKU 的销量（保存是「先删后建」，id 会变；P4 后销量有真实业务含义，不能丢）
        Map<String, Integer> oldSalesByCode = new HashMap<>();
        Integer oldSingleSales = null;
        List<MallSku> oldSkus = mallSkuMapper.listByProductId(productId);
        for (MallSku old : oldSkus) {
            if (old.getSales() != null) {
                if (hasText(old.getSkuCode())) {
                    oldSalesByCode.put(old.getSkuCode().trim(), old.getSales());
                }
                oldSingleSales = old.getSales();
            }
        }
        boolean carrySingleSales = oldSkus.size() == 1;

        clearProductRelations(productId);
        insertImages(productId, images, now);

        Map<Long, Map<String, Long>> valueIdBySpec = insertSpecs(productId, specs, specValues, now);
        insertSkus(productId, skus, specValues, specs, valueIdBySpec, now,
                oldSalesByCode, carrySingleSales ? oldSingleSales : null);

        // 7) 由 SKU 重算 SPU 上的冗余字段（价格区间、总库存）
        mallProductMapper.refreshAggregates(productId);

        // 8) 同步新 SKU 的库存到 Redis（保存是「先删后建」，SKU id 会变，旧 key 作废）
        mallStockService.syncProductSkuStock(productId);

        // 9) Cache-Aside：先写库，再删缓存（新增时 id 是新生成的，删也无害）
        evictCache(productId);

        log.info("保存商品：id={}, name={}, 规格项={}, SKU={}",
                productId, dto.getName(), specs.size(), skus.size());
        return productId;
    }

    private void clearProductRelations(Long productId) {
        List<Long> oldSkuIds = mallSkuMapper.listByProductId(productId).stream()
                .map(MallSku::getId).toList();
        if (!oldSkuIds.isEmpty()) {
            mallSkuSpecMapper.deleteBySkuIds(oldSkuIds);
        }
        mallSkuMapper.deleteByProductId(productId);

        List<Long> oldSpecIds = mallSpecMapper.listByProductId(productId).stream()
                .map(MallSpec::getId).toList();
        if (!oldSpecIds.isEmpty()) {
            mallSpecValueMapper.deleteBySpecIds(oldSpecIds);
        }
        mallSpecMapper.deleteByProductId(productId);

        mallProductImageMapper.deleteByProductId(productId);
    }

    private void insertImages(Long productId, List<String> images, LocalDateTime now) {
        if (images.isEmpty()) {
            return;
        }
        List<MallProductImage> list = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            list.add(MallProductImage.builder()
                    .productId(productId)
                    .url(images.get(i))
                    .sort(i)
                    .createTime(now)
                    .build());
        }
        mallProductImageMapper.insertBatch(list);
    }

    /**
     * 写规格项与规格值，返回 {@code specIndex -> (规格值 -> valueId)} 供建 SKU 关联用。
     */
    private Map<Long, Map<String, Long>> insertSpecs(Long productId,
                                                     List<MallProductSaveDTO.SpecDTO> specs,
                                                     List<List<String>> specValues,
                                                     LocalDateTime now) {
        // key 用 specId，value 是该规格下「规格值 -> valueId」
        Map<Long, Map<String, Long>> valueIdBySpec = new LinkedHashMap<>();
        for (int i = 0; i < specs.size(); i++) {
            MallProductSaveDTO.SpecDTO s = specs.get(i);
            MallSpec spec = MallSpec.builder()
                    .productId(productId)
                    .name(s.getName().trim())
                    .sort(i)
                    .createTime(now)
                    .build();
            mallSpecMapper.insert(spec);   // 回填 spec.id

            Map<String, Long> map = new LinkedHashMap<>();
            List<String> values = specValues.get(i);
            for (int j = 0; j < values.size(); j++) {
                MallSpecValue v = MallSpecValue.builder()
                        .specId(spec.getId())
                        .value(values.get(j))
                        .sort(j)
                        .createTime(now)
                        .build();
                mallSpecValueMapper.insert(v);   // 回填 v.id
                map.put(values.get(j), v.getId());
            }
            valueIdBySpec.put(spec.getId(), map);
        }
        return valueIdBySpec;
    }

    private void insertSkus(Long productId,
                            List<MallProductSaveDTO.SkuDTO> skus,
                            List<List<String>> specValues,
                            List<MallProductSaveDTO.SpecDTO> specs,
                            Map<Long, Map<String, Long>> valueIdBySpec,
                            LocalDateTime now,
                            Map<String, Integer> oldSalesByCode,
                            Integer carrySingleSales) {
        List<Long> specIds = new ArrayList<>(valueIdBySpec.keySet());
        List<MallSkuSpec> links = new ArrayList<>();

        for (MallProductSaveDTO.SkuDTO dto : skus) {
            MallSku sku = MallSku.builder()
                    .productId(productId)
                    .skuCode(hasText(dto.getSkuCode()) ? dto.getSkuCode().trim() : null)
                    .price(dto.getPrice())
                    .stock(dto.getStock())
                    .sales(carryOverSales(dto.getSkuCode(), oldSalesByCode, carrySingleSales))
                    .image(dto.getImage())
                    .status(dto.getStatus() == null ? 1 : dto.getStatus())
                    .createTime(now)
                    .updateTime(now)
                    .build();
            mallSkuMapper.insert(sku);   // 回填 sku.id

            // 无规格商品：不建任何关联（它就是那条"裸 SKU"）
            if (specs.isEmpty()) {
                continue;
            }
            List<String> picks = dto.getSpecValues();
            for (int i = 0; i < specIds.size(); i++) {
                Long specId = specIds.get(i);
                Long valueId = valueIdBySpec.get(specId).get(picks.get(i).trim());
                links.add(MallSkuSpec.builder()
                        .skuId(sku.getId())
                        .specId(specId)
                        .specValueId(valueId)
                        .build());
            }
        }
        if (!links.isEmpty()) {
            mallSkuSpecMapper.insertBatch(links);
        }
    }

    // ============================================================
    // 校验
    // ============================================================

    /** 返回归一化后的规格值（去空格），顺序与 specs 一致 */
    private List<List<String>> normalizeSpecs(List<MallProductSaveDTO.SpecDTO> specs) {
        if (specs.size() > MAX_SPEC_COUNT) {
            throw new MallBusinessException(MallMessageConstant.SPEC_TOO_MANY);
        }
        List<List<String>> normalized = new ArrayList<>();
        Set<String> specNames = new HashSet<>();
        for (MallProductSaveDTO.SpecDTO s : specs) {
            String name = s.getName() == null ? "" : s.getName().trim();
            if (name.isEmpty()) {
                throw new MallBusinessException("规格名不能为空");
            }
            if (!specNames.add(name)) {
                throw new MallBusinessException(MallMessageConstant.SPEC_NAME_DUPLICATE);
            }
            List<String> values = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            List<String> raw = s.getValues() == null ? List.of() : s.getValues();
            for (String r : raw) {
                String v = r == null ? "" : r.trim();
                if (v.isEmpty()) {
                    continue;
                }
                if (!seen.add(v)) {
                    throw new MallBusinessException(MallMessageConstant.SPEC_VALUE_DUPLICATE);
                }
                values.add(v);
            }
            if (values.isEmpty()) {
                throw new MallBusinessException(MallMessageConstant.SPEC_VALUE_EMPTY);
            }
            normalized.add(values);
        }
        return normalized;
    }

    /**
     * 校验 SKU 组合是完整的笛卡尔积：
     * <ul>
     *   <li>无规格 → 恰好 1 个 SKU，且 specValues 必须为空</li>
     *   <li>有规格 → SKU 数 = 各规格值数量的乘积；每个 SKU 的 specValues 长度 = 规格项数；
     *       每个值必须属于对应规格项；组合不能重复</li>
     * </ul>
     */
    private void validateSkuMatrix(List<List<String>> specValues, List<MallProductSaveDTO.SkuDTO> skus) {
        if (skus.isEmpty()) {
            throw new MallBusinessException(MallMessageConstant.SKU_REQUIRED);
        }

        if (specValues.isEmpty()) {
            if (skus.size() != 1) {
                throw new MallBusinessException(MallMessageConstant.SKU_NO_SPEC_EXACTLY_ONE);
            }
            List<String> picks = skus.get(0).getSpecValues();
            if (picks != null && picks.stream().anyMatch(this::hasText)) {
                throw new MallBusinessException(MallMessageConstant.SPEC_VALUE_UNKNOWN);
            }
            return;
        }

        long expected = 1;
        for (List<String> values : specValues) {
            expected *= values.size();
        }
        if (skus.size() != expected) {
            throw new MallBusinessException(MallMessageConstant.SKU_MATRIX_MISMATCH
                    + "（应为 " + expected + " 个，实际 " + skus.size() + " 个）");
        }

        Set<String> combos = new HashSet<>();
        for (MallProductSaveDTO.SkuDTO sku : skus) {
            List<String> picks = sku.getSpecValues();
            if (picks == null || picks.size() != specValues.size()) {
                throw new MallBusinessException(MallMessageConstant.SKU_MATRIX_MISMATCH
                        + "（每个 SKU 需要 " + specValues.size() + " 个规格值）");
            }
            List<String> ordered = new ArrayList<>();
            for (int i = 0; i < picks.size(); i++) {
                String v = picks.get(i) == null ? "" : picks.get(i).trim();
                if (!specValues.get(i).contains(v)) {
                    throw new MallBusinessException(MallMessageConstant.SPEC_VALUE_UNKNOWN
                            + "：" + v);
                }
                ordered.add(v);
            }
            if (!combos.add(String.join("|", ordered))) {
                throw new MallBusinessException(MallMessageConstant.SKU_DUPLICATE
                        + "：" + String.join("/", ordered));
            }
        }
    }

    // ============================================================
    // 上下架 / 删除
    // ============================================================

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        requireProduct(id);
        if (status == null || (status != STATUS_ON_SALE && status != STATUS_OFF_SALE)) {
            throw new MallBusinessException("商品状态只能是 1（上架）或 0（下架）");
        }
        if (status == STATUS_ON_SALE && mallSkuMapper.listByProductId(id).isEmpty()) {
            throw new MallBusinessException("商品没有 SKU，不能上架");
        }
        mallProductMapper.updateStatus(id, status);
        // 上下架直接影响公开详情（status 字段在 VO 里），必须删缓存
        evictCache(id);
        log.info("商品上下架：id={}, status={}", id, status);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        MallProduct product = requireProduct(id);
        if (STATUS_ON_SALE == (product.getStatus() == null ? 0 : product.getStatus())) {
            throw new MallBusinessException(MallMessageConstant.PRODUCT_ON_SALE_CANNOT_DELETE);
        }
        // 先删关联（sku_spec 依赖 sku），再删各自的表，最后删 SPU
        mallSkuSpecMapper.deleteByProductId(id);
        mallSkuMapper.deleteByProductId(id);
        List<Long> specIds = mallSpecMapper.listByProductId(id).stream().map(MallSpec::getId).toList();
        if (!specIds.isEmpty()) {
            mallSpecValueMapper.deleteBySpecIds(specIds);
        }
        mallSpecMapper.deleteByProductId(id);
        mallProductImageMapper.deleteByProductId(id);
        mallProductMapper.deleteById(id);
        evictCache(id);
        log.info("删除商品：id={}", id);
    }

    /** Cache-Aside：商品有写操作就删掉详情缓存，下次读回源 */
    private void evictCache(Long productId) {
        cacheClient.delete(CACHE_KEY_PREFIX + productId);
        log.debug("[mall] 商品写操作，删除详情缓存：productId={}", productId);
    }

    private MallProduct requireProduct(Long id) {
        MallProduct p = mallProductMapper.getById(id);
        if (p == null) {
            throw new MallBusinessException(MallMessageConstant.PRODUCT_NOT_FOUND);
        }
        return p;
    }

    /** 分类必须存在、启用、且是叶子节点 */
    private MallCategory requireLeafCategory(Long categoryId) {
        MallCategory category = mallCategoryMapper.getById(categoryId);
        if (category == null) {
            throw new MallBusinessException(MallMessageConstant.CATEGORY_NOT_FOUND);
        }
        if (Integer.valueOf(0).equals(category.getStatus())) {
            throw new MallBusinessException("分类已禁用，不能挂商品");
        }
        if (!mallCategoryMapper.listChildren(categoryId).isEmpty()) {
            throw new MallBusinessException(MallMessageConstant.CATEGORY_NOT_LEAF);
        }
        return category;
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * 重建 SKU 时尽量把旧销量带过来：
     * 优先按 {@code skuCode} 匹配；旧商品只有一个 SKU（无规格）时直接沿用。
     * 匹配不上的按 0（新增的 SKU 本来就没销量）。
     */
    private int carryOverSales(String newSkuCode,
                               Map<String, Integer> oldSalesByCode,
                               Integer carrySingleSales) {
        if (hasText(newSkuCode)) {
            Integer byCode = oldSalesByCode.get(newSkuCode.trim());
            if (byCode != null) {
                return byCode;
            }
        }
        return carrySingleSales == null ? 0 : carrySingleSales;
    }
}
