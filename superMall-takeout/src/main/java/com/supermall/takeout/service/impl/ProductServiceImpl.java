package com.supermall.takeout.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.constant.MessageConstant;
import com.supermall.takeout.constant.RedisKeyConstant;
import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.ProductDTO;
import com.supermall.takeout.dto.ProductPageQueryDTO;
import com.supermall.takeout.dto.ProductQueryDTO;
import com.supermall.takeout.dto.SkuDTO;
import com.supermall.takeout.enumeration.BusinessType;
import com.supermall.takeout.entity.Category;
import com.supermall.takeout.entity.Product;
import com.supermall.takeout.entity.Sku;
import com.supermall.takeout.exception.ProductBusinessException;
import com.supermall.takeout.mapper.CategoryMapper;
import com.supermall.takeout.mapper.ProductMapper;
import com.supermall.takeout.mapper.SkuMapper;
import com.supermall.result.PageResult;
import com.supermall.utils.RichTextSanitizer;
import com.supermall.takeout.service.ProductService;
import com.supermall.takeout.service.StockService;
import com.supermall.takeout.utils.RedisCacheClient;
import com.supermall.takeout.vo.ProductVO;
import com.supermall.takeout.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品服务：集成了 Redis 缓存的三种经典问题处理。
 * <p>
 * 防穿透：查不到的数据缓存 NULL 短 TTL
 * 防击穿：热点 key 过期时用 setnx 互斥锁 + 双重检查，只放一个请求打到 DB
 * 防雪崩：缓存 TTL 加随机抖动
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String CACHE_KEY_PREFIX = RedisKeyConstant.PRODUCT_CACHE_PREFIX;
    private static final long CACHE_BASE_SECONDS = 300;
    private static final long CACHE_JITTER_SECONDS = 120;
    private static final long NULL_CACHE_SECONDS = 60;

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;
    private final RedisCacheClient cacheClient;
    private final StockService stockService;

    @Override
    @Transactional
    public void save(ProductDTO dto) {
        Long merchantId = BaseContext.getCurrentId();
        Category category = requireCategory(dto.getCategoryId(), merchantId);
        LocalDateTime now = LocalDateTime.now();

        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setId(null);
        product.setMerchantId(merchantId);
        // 商品描述是纯文本：入库前剥掉标签（防存储型 XSS）
        product.setDescription(RichTextSanitizer.plainText(product.getDescription()));
        // 业态由分类派生，忽略前端传值
        product.setBusinessType(category.getBusinessType());
        product.setSales(0);
        product.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        product.setCreateTime(now);
        product.setUpdateTime(now);
        productMapper.insert(product);

        saveSkus(dto.getSkuList(), product.getId(), merchantId, now);
        stockService.syncProductStock(product.getId(), product.getStock());
    }

    @Override
    @Transactional
    public void update(ProductDTO dto) {
        Long merchantId = BaseContext.getCurrentId();
        Product exist = requireOwnedProduct(dto.getId(), merchantId);
        Category category = requireCategory(dto.getCategoryId(), merchantId);

        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setMerchantId(merchantId);
        product.setDescription(RichTextSanitizer.plainText(product.getDescription()));
        // 业态跟随所选分类变更
        product.setBusinessType(category.getBusinessType());
        productMapper.update(product);

        if (dto.getSkuList() != null) {
            skuMapper.deleteByProductId(dto.getId(), merchantId);
            saveSkus(dto.getSkuList(), dto.getId(), merchantId, LocalDateTime.now());
        }
        stockService.syncProductStock(dto.getId(), dto.getStock());
        // Cache-Aside：先写库，再删缓存
        cacheClient.delete(CACHE_KEY_PREFIX + dto.getId());
        log.info("商品更新，删除缓存：productId={}", dto.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long merchantId = BaseContext.getCurrentId();
        Product exist = requireOwnedProduct(id, merchantId);
        if (Integer.valueOf(1).equals(exist.getStatus())) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_ON_SALE);
        }
        skuMapper.deleteByProductId(id, merchantId);
        productMapper.deleteById(id, merchantId);
        cacheClient.delete(CACHE_KEY_PREFIX + id);
    }

    @Override
    public ProductVO getForMerchant(Long id) {
        Product product = requireOwnedProduct(id, BaseContext.getCurrentId());
        return buildVO(product);
    }

    @Override
    public ProductVO getDetail(Long id) {
        String key = CACHE_KEY_PREFIX + id;
        TypeReference<ProductVO> type = new TypeReference<>() {
        };

        // 1. 先查缓存
        ProductVO cached = cacheClient.getJson(key, type);
        if (cached != null) {
            return cached;
        }
        // 2. 命中空值标记：直接判定不存在，挡住穿透
        if (cacheClient.isNullMarked(key)) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }

        // 3. 未命中：用互斥锁防击穿，只让一个请求回源
        boolean locked = cacheClient.tryLock(key, 10);
        if (!locked) {
            // 没抢到锁：短暂等待后重试读缓存，仍没有则回源（避免无限等待）
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ProductVO retry = cacheClient.getJson(key, type);
            if (retry != null) {
                return retry;
            }
            return buildVO(requireOnSaleProduct(id));
        }

        try {
            // 双重检查：可能已被其他线程写入
            ProductVO doubleCheck = cacheClient.getJson(key, type);
            if (doubleCheck != null) {
                return doubleCheck;
            }
            Product product = productMapper.getById(id);
            if (product == null || Integer.valueOf(0).equals(product.getStatus())) {
                cacheClient.setNull(key, NULL_CACHE_SECONDS);
                throw new ProductBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
            }
            ProductVO vo = buildVO(product);
            cacheClient.setJson(key, vo, CACHE_BASE_SECONDS, CACHE_JITTER_SECONDS);
            return vo;
        } finally {
            cacheClient.unlock(key);
        }
    }

    @Override
    public PageResult pageQuery(ProductPageQueryDTO dto) {
        dto.setMerchantId(BaseContext.getCurrentId());
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<Product> page = productMapper.pageQuery(dto);
        List<ProductVO> list = page.getResult().stream()
                .map(this::buildVO)
                .collect(Collectors.toList());
        return new PageResult(page.getTotal(), list);
    }

    @Override
    public PageResult pageQueryForUser(ProductQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<Product> page = productMapper.pageQueryForUser(dto);
        List<ProductVO> list = page.getResult().stream()
                .map(this::buildVO)
                .collect(Collectors.toList());
        return new PageResult(page.getTotal(), list);
    }

    @Override
    public List<ProductVO> randomFeed(String keyword, Integer businessType, int limit) {
        if (businessType != null && !BusinessType.isValid(businessType)) {
            throw new ProductBusinessException(MessageConstant.BUSINESS_TYPE_INVALID);
        }
        int n = Math.min(Math.max(limit, 1), 60);
        return productMapper.randomOnSale(keyword, businessType, n).stream()
                .map(this::buildVO)
                .collect(Collectors.toList());
    }

    /* ---------------- 私有方法 ---------------- */

    private void saveSkus(List<SkuDTO> skuList, Long productId, Long merchantId, LocalDateTime now) {
        if (skuList == null || skuList.isEmpty()) {
            return;
        }
        List<Sku> skus = new ArrayList<>();
        for (SkuDTO s : skuList) {
            if (Integer.valueOf(0).equals(s.getStatus())) {
                continue;
            }
            Sku sku = Sku.builder()
                    .productId(productId)
                    .merchantId(merchantId)
                    .name(s.getName())
                    .price(s.getPrice())
                    .stock(s.getStock())
                    .status(s.getStatus() == null ? 1 : s.getStatus())
                    .createTime(now)
                    .updateTime(now)
                    .build();
            skus.add(sku);
        }
        if (!skus.isEmpty()) {
            skuMapper.batchInsert(skus);
            for (Sku sku : skus) {
                stockService.syncSkuStock(sku.getId(), sku.getStock());
            }
        }
    }

    private Product requireOwnedProduct(Long id, Long merchantId) {
        Product product = productMapper.getById(id);
        if (product == null || !merchantId.equals(product.getMerchantId())) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private Product requireOnSaleProduct(Long id) {
        Product product = productMapper.getById(id);
        if (product == null || Integer.valueOf(0).equals(product.getStatus())) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private Category requireCategory(Long categoryId, Long merchantId) {
        Category category = categoryMapper.getById(categoryId, merchantId);
        if (category == null) {
            throw new ProductBusinessException(MessageConstant.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private ProductVO buildVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        Category category = categoryMapper.getById(product.getCategoryId(), product.getMerchantId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        List<Sku> skus = skuMapper.listByProductId(product.getId());
        vo.setSkuList(skus.stream().map(s -> {
            SkuVO sv = new SkuVO();
            BeanUtils.copyProperties(s, sv);
            return sv;
        }).collect(Collectors.toList()));
        return vo;
    }
}
