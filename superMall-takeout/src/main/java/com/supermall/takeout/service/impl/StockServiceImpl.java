package com.supermall.takeout.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.takeout.constant.RedisKeyConstant;
import com.supermall.takeout.entity.CartItem;
import com.supermall.takeout.entity.OrderItem;
import com.supermall.takeout.entity.Product;
import com.supermall.takeout.entity.Sku;
import com.supermall.takeout.exception.OrderBusinessException;
import com.supermall.takeout.mapper.ProductMapper;
import com.supermall.takeout.mapper.SkuMapper;
import com.supermall.takeout.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存服务实现：Lua 脚本保证「检查 + 扣减」原子性，支持商品级与 SKU 级库存。
 * 有规格扣 SKU 库存，无规格扣商品库存。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private static final String PRODUCT_PREFIX = RedisKeyConstant.PRODUCT_STOCK_PREFIX;
    private static final String SKU_PREFIX = RedisKeyConstant.SKU_STOCK_PREFIX;

    private final StringRedisTemplate stringRedisTemplate;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    /** 预扣脚本：由 RedisScriptConfig 从 .lua 文件加载为 Bean */
    private final DefaultRedisScript<Long> deductStockScript;
    /** 回补脚本：由 RedisScriptConfig 从 .lua 文件加载为 Bean */
    private final DefaultRedisScript<Long> releaseStockScript;

    private String keyOf(Long productId, Long skuId) {
        if (skuId != null && skuId > 0) {
            return SKU_PREFIX + skuId;
        }
        return PRODUCT_PREFIX + productId;
    }

    @Override
    public void deductStock(List<CartItem> items) {
        // 先聚合，避免同一商品/SKU 被拆成多次扣减
        Map<String, Integer> qtyMap = new LinkedHashMap<>();
        for (CartItem it : items) {
            qtyMap.merge(keyOf(it.getProductId(), it.getSkuId()), it.getNumber(), Integer::sum);
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(qtyMap.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            Long result = stringRedisTemplate.execute(
                    deductStockScript,
                    Collections.singletonList(entry.getKey()),
                    String.valueOf(entry.getValue()));

            if (result == null) {
                throw new OrderBusinessException("库存扣减异常，请稍后重试");
            }
            if (result == -2L) {
                throw new OrderBusinessException(MessageConstant.STOCK_NOT_WARMED);
            }
            if (result == -1L) {
                // 回补前面已经扣掉的部分，保证整体不欠库存
                for (int j = 0; j < i; j++) {
                    Map.Entry<String, Integer> prev = entries.get(j);
                    releaseKey(prev.getKey(), prev.getValue());
                }
                throw new OrderBusinessException(MessageConstant.STOCK_NOT_ENOUGH);
            }
        }
    }

    @Override
    public void releaseStock(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<String, Integer> qtyMap = new LinkedHashMap<>();
        for (OrderItem it : items) {
            qtyMap.merge(keyOf(it.getProductId(), it.getSkuId()), it.getNumber(), Integer::sum);
        }
        for (Map.Entry<String, Integer> e : qtyMap.entrySet()) {
            releaseKey(e.getKey(), e.getValue());
        }
    }

    private void releaseKey(String key, Integer number) {
        stringRedisTemplate.execute(releaseStockScript, Collections.singletonList(key), String.valueOf(number));
    }

    @Override
    public void warmUpAll() {
        List<Product> products = productMapper.listAll();
        for (Product p : products) {
            if (p.getStock() != null) {
                stringRedisTemplate.opsForValue().set(PRODUCT_PREFIX + p.getId(), String.valueOf(p.getStock()));
            }
        }
        List<Sku> skus = skuMapper.listAll();
        for (Sku s : skus) {
            if (s.getStock() != null) {
                stringRedisTemplate.opsForValue().set(SKU_PREFIX + s.getId(), String.valueOf(s.getStock()));
            }
        }
        log.info("库存预热完成：商品 {} 个，规格 {} 个", products.size(), skus.size());
    }

    @Override
    public void syncProductStock(Long productId, Integer stock) {
        if (productId == null || stock == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(PRODUCT_PREFIX + productId, String.valueOf(stock));
    }

    @Override
    public void syncSkuStock(Long skuId, Integer stock) {
        if (skuId == null || stock == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(SKU_PREFIX + skuId, String.valueOf(stock));
    }
}
