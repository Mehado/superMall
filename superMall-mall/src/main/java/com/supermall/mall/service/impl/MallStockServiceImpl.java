package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.constant.MallRedisKeyConstant;
import com.supermall.mall.entity.MallOrderItem;
import com.supermall.mall.entity.MallSku;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallProductMapper;
import com.supermall.mall.mapper.MallSkuMapper;
import com.supermall.mall.service.MallStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存服务实现（Redis 预扣 + DB 条件扣减）。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallStockServiceImpl implements MallStockService {

    private static final String SKU_PREFIX = MallRedisKeyConstant.SKU_STOCK_PREFIX;

    private final StringRedisTemplate stringRedisTemplate;
    private final MallSkuMapper mallSkuMapper;
    private final MallProductMapper mallProductMapper;
    /** 预扣脚本：MallRedisScriptConfig 从 .lua 加载 */
    private final DefaultRedisScript<Long> mallDeductStockScript;
    /** 回补脚本 */
    private final DefaultRedisScript<Long> mallReleaseStockScript;

    // ============================================================
    // 预热 / 同步
    // ============================================================

    @Override
    public void warmUpAll() {
        List<MallSku> skus = mallSkuMapper.listAll();
        for (MallSku s : skus) {
            if (s.getStock() != null) {
                stringRedisTemplate.opsForValue().set(SKU_PREFIX + s.getId(), String.valueOf(s.getStock()));
            }
        }
        log.info("[mall] 库存预热完成：SKU {} 个", skus.size());
    }

    @Override
    public void syncProductSkuStock(Long productId) {
        for (MallSku s : mallSkuMapper.listByProductId(productId)) {
            syncSkuStock(s.getId(), s.getStock());
        }
    }

    @Override
    public void syncSkuStock(Long skuId, Integer stock) {
        if (skuId == null || stock == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(SKU_PREFIX + skuId, String.valueOf(stock));
    }

    // ============================================================
    // Redis 预扣 / 回补
    // ============================================================

    @Override
    public void deductRedis(List<MallOrderItem> items) {
        Map<Long, Integer> qty = aggregateBySku(items);
        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(qty.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<Long, Integer> e = entries.get(i);
            Long result = stringRedisTemplate.execute(
                    mallDeductStockScript,
                    Collections.singletonList(SKU_PREFIX + e.getKey()),
                    String.valueOf(e.getValue()));

            if (result == null) {
                rollbackRedis(entries, i);
                throw new MallBusinessException(MallMessageConstant.STOCK_DEDUCT_ERROR);
            }
            if (result == -2L) {
                rollbackRedis(entries, i);
                throw new MallBusinessException(MallMessageConstant.STOCK_NOT_WARMED);
            }
            if (result == -1L) {
                rollbackRedis(entries, i);
                throw new MallBusinessException(MallMessageConstant.STOCK_NOT_ENOUGH);
            }
        }
    }

    /** 回补前 i 个已扣的 key（第 i 个失败时用） */
    private void rollbackRedis(List<Map.Entry<Long, Integer>> entries, int endExclusive) {
        for (int j = 0; j < endExclusive; j++) {
            Map.Entry<Long, Integer> prev = entries.get(j);
            releaseKey(prev.getKey(), prev.getValue());
        }
    }

    @Override
    public void releaseRedis(List<MallOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, Integer> e : aggregateBySku(items).entrySet()) {
            releaseKey(e.getKey(), e.getValue());
        }
    }

    private void releaseKey(Long skuId, Integer number) {
        stringRedisTemplate.execute(mallReleaseStockScript,
                Collections.singletonList(SKU_PREFIX + skuId), String.valueOf(number));
    }

    // ============================================================
    // DB 条件扣减 / 回补（调用方事务的一部分）
    // ============================================================

    @Override
    @Transactional
    public void deductDb(List<MallOrderItem> items) {
        for (MallOrderItem item : items) {
            int rows = mallSkuMapper.deductStock(item.getSkuId(), item.getNumber());
            if (rows == 0) {
                // 抛异常 → 外层事务回滚前面已扣的 SKU；Redis 由调用方回补
                throw new MallBusinessException(MallMessageConstant.STOCK_NOT_ENOUGH);
            }
        }
    }

    @Override
    @Transactional
    public void releaseDb(List<MallOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (MallOrderItem item : items) {
            mallSkuMapper.releaseStock(item.getSkuId(), item.getNumber());
        }
    }

    @Override
    public void refreshAggregates(List<MallOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, Boolean> seen = new LinkedHashMap<>();
        for (MallOrderItem item : items) {
            if (item.getProductId() != null && seen.putIfAbsent(item.getProductId(), Boolean.TRUE) == null) {
                mallProductMapper.refreshAggregates(item.getProductId());
            }
        }
    }

    @Override
    @Transactional
    public void adjustSales(List<MallOrderItem> items, int delta) {
        for (MallOrderItem item : items) {
            if (item.getSkuId() != null) {
                mallSkuMapper.incrementSales(item.getSkuId(), delta * item.getNumber());
            }
            if (item.getProductId() != null) {
                mallProductMapper.incrementSales(item.getProductId(), delta * item.getNumber());
            }
        }
    }

    // ============================================================
    // 私有
    // ============================================================

    private Map<Long, Integer> aggregateBySku(List<MallOrderItem> items) {
        Map<Long, Integer> qty = new LinkedHashMap<>();
        if (items == null) {
            return qty;
        }
        for (MallOrderItem item : items) {
            qty.merge(item.getSkuId(), item.getNumber(), Integer::sum);
        }
        return qty;
    }
}
