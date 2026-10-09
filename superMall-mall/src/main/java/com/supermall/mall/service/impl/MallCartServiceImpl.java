package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.entity.MallCartItem;
import com.supermall.mall.entity.MallProduct;
import com.supermall.mall.entity.MallSku;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallCartItemMapper;
import com.supermall.mall.mapper.MallProductMapper;
import com.supermall.mall.mapper.MallSkuMapper;
import com.supermall.mall.service.MallCartService;
import com.supermall.mall.utils.MallCartValidators;
import com.supermall.mall.vo.MallCartAddVO;
import com.supermall.mall.vo.MallCartItemVO;
import com.supermall.mall.vo.MallCartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车实现。
 *
 * <p>两个核心口径：
 * <ul>
 *   <li><b>价格实时计算</b>：列表 JOIN {@code mall_sku} 取当前价，不存快照。
 *       购物车行里的 price 永远是「现在下单要付的钱」。</li>
 *   <li><b>失效商品不删不选</b>：商品下架/SKU 失效/售罄/库存不足的行照常返回、
 *       带 {@code invalidReason}，但不计入结算金额，也不允许选中。</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallCartServiceImpl implements MallCartService {

    private static final int SELECTED = 1;
    private static final int UNSELECTED = 0;
    private static final int STATUS_ON_SALE = 1;
    private static final int STATUS_ENABLED = 1;
    private static final String SCOPE_ALL = "all";
    private static final String SCOPE_INVALID = "invalid";

    private final MallCartItemMapper mallCartItemMapper;
    private final MallSkuMapper mallSkuMapper;
    private final MallProductMapper mallProductMapper;

    // ============================================================
    // 查询
    // ============================================================

    @Override
    public MallCartVO get(Long userId) {
        List<MallCartItemVO> items = mallCartItemMapper.listByUser(userId);

        int totalNumber = 0;
        int validCount = 0;
        int invalidCount = 0;
        int selectedCount = 0;
        int selectedNumber = 0;
        BigDecimal selectedAmount = BigDecimal.ZERO;

        for (MallCartItemVO item : items) {
            int number = item.getNumber() == null ? 0 : item.getNumber();
            totalNumber += number;

            String reason = MallCartValidators.invalidReason(item);
            boolean valid = reason == null;
            item.setValid(valid);
            item.setInvalidReason(reason);
            item.setSubtotal(subtotal(item.getPrice(), number));

            if (!valid) {
                invalidCount++;
                // 失效行对外一律显示「未选中」：它既不能结算、也不能被勾选，
                // 返回 1 只会让前端困惑（数据库里的原值不动，商品恢复后照旧生效）
                item.setSelected(UNSELECTED);
                continue;
            }
            validCount++;
            if (Integer.valueOf(SELECTED).equals(item.getSelected())) {
                selectedCount++;
                selectedNumber += number;
                selectedAmount = selectedAmount.add(subtotal(item.getPrice(), number));
            }
        }

        return MallCartVO.builder()
                .items(items)
                .totalItems(items.size())
                .totalNumber(totalNumber)
                .validCount(validCount)
                .invalidCount(invalidCount)
                .selectedCount(selectedCount)
                .selectedNumber(selectedNumber)
                .selectedAmount(selectedAmount)
                // 没有有效行时不算「全选」，否则前端空购物车会显示勾选
                .allSelected(validCount > 0 && selectedCount == validCount)
                .build();
    }

    @Override
    public int count(Long userId) {
        return mallCartItemMapper.countByUser(userId);
    }

    @Override
    public int invalidCount(Long userId) {
        return mallCartItemMapper.countInvalidByUser(userId);
    }

    // ============================================================
    // 加购 / 改数量
    // ============================================================

    @Override
    @Transactional
    public MallCartAddVO add(Long userId, Long skuId, Integer number) {
        int addNumber = number == null ? 1 : number;
        if (addNumber < 1) {
            throw new MallBusinessException("数量至少为 1");
        }

        MallSku sku = mallSkuMapper.getById(skuId);
        if (sku == null) {
            throw new MallBusinessException(MallMessageConstant.CART_SKU_NOT_FOUND);
        }
        MallProduct product = mallProductMapper.getById(sku.getProductId());
        if (product == null) {
            throw new MallBusinessException(MallMessageConstant.CART_SKU_NOT_FOUND);
        }
        if (!Integer.valueOf(STATUS_ON_SALE).equals(product.getStatus())) {
            throw new MallBusinessException(MallMessageConstant.CART_PRODUCT_OFF_SALE);
        }
        if (!Integer.valueOf(STATUS_ENABLED).equals(sku.getStatus())) {
            throw new MallBusinessException(MallMessageConstant.CART_SKU_DISABLED);
        }
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        if (stock <= 0) {
            throw new MallBusinessException(MallMessageConstant.CART_OUT_OF_STOCK);
        }

        // 一条原子 SQL 完成「插入或累加」，并用 LEAST(..., stock) 截断到库存上限。
        // 并发加购同一 SKU 不会插出两行，也不会把数量加到超过库存。
        MallCartItem before = mallCartItemMapper.getByUserAndSku(userId, skuId);
        int already = before == null ? 0 : (before.getNumber() == null ? 0 : before.getNumber());
        mallCartItemMapper.upsertAdd(userId, sku.getProductId(), skuId, addNumber, stock);

        MallCartItem after = mallCartItemMapper.getByUserAndSku(userId, skuId);
        int finalNumber = after == null ? addNumber : after.getNumber();
        if (already + addNumber > stock) {
            log.info("加购被库存截断：userId={}, skuId={}, 期望={}, 实际={}, 库存={}",
                    userId, skuId, already + addNumber, finalNumber, stock);
        }
        return MallCartAddVO.builder()
                .id(after == null ? null : after.getId())
                .skuId(skuId)
                .number(finalNumber)
                .build();
    }

    @Override
    @Transactional
    public void updateNumber(Long userId, Long id, Integer number) {
        if (number == null || number < 1) {
            throw new MallBusinessException("数量至少为 1");
        }
        MallCartItem item = requireOwned(userId, id);
        MallSku sku = mallSkuMapper.getById(item.getSkuId());
        if (sku == null || !Integer.valueOf(STATUS_ENABLED).equals(sku.getStatus())) {
            throw new MallBusinessException(MallMessageConstant.CART_SKU_NOT_FOUND);
        }
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        if (stock <= 0) {
            throw new MallBusinessException(MallMessageConstant.CART_OUT_OF_STOCK);
        }
        if (number > stock) {
            throw new MallBusinessException(MallMessageConstant.CART_STOCK_NOT_ENOUGH + stock + " 件");
        }
        mallCartItemMapper.updateNumber(id, userId, number);
    }

    // ============================================================
    // 勾选
    // ============================================================

    @Override
    @Transactional
    public void updateSelected(Long userId, Long id, Integer selected) {
        int value = normalizeSelected(selected);
        MallCartItem item = requireOwned(userId, id);
        if (value == SELECTED) {
            // 失效商品不允许被选中：否则结算金额会莫名其妙
            String reason = invalidReasonOf(item);
            if (reason != null) {
                throw new MallBusinessException(MallMessageConstant.CART_ITEM_INVALID + "（" + reason + "）");
            }
        }
        mallCartItemMapper.updateSelected(id, userId, value);
    }

    @Override
    @Transactional
    public void updateAllSelected(Long userId, Integer selected) {
        int value = normalizeSelected(selected);
        if (value == SELECTED) {
            // 只勾有效行：失效商品不能被一键全选带进结算
            mallCartItemMapper.updateSelectedValid(userId, SELECTED);
        } else {
            mallCartItemMapper.updateSelectedAll(userId, UNSELECTED);
        }
    }

    // ============================================================
    // 删除 / 清理
    // ============================================================

    @Override
    @Transactional
    public void remove(Long userId, Long id) {
        // 幂等：删一个本来就不存在的行也返回成功
        mallCartItemMapper.deleteById(id, userId);
    }

    @Override
    @Transactional
    public void clear(Long userId, String scope) {
        String s = (scope == null || scope.isBlank()) ? SCOPE_ALL : scope.trim().toLowerCase();
        switch (s) {
            case SCOPE_ALL -> mallCartItemMapper.deleteAll(userId);
            case SCOPE_INVALID -> mallCartItemMapper.deleteInvalid(userId);
            default -> throw new MallBusinessException(MallMessageConstant.CART_SCOPE_INVALID);
        }
    }

    // ============================================================
    // 私有工具
    // ============================================================

    private MallCartItem requireOwned(Long userId, Long id) {
        MallCartItem item = id == null ? null : mallCartItemMapper.getById(id);
        if (item == null || !userId.equals(item.getUserId())) {
            throw new MallBusinessException(MallMessageConstant.CART_ITEM_NOT_FOUND);
        }
        return item;
    }

    private int normalizeSelected(Integer selected) {
        if (selected == null || (selected != SELECTED && selected != UNSELECTED)) {
            throw new MallBusinessException("选中状态只能是 1 或 0");
        }
        return selected;
    }

    /** 对购物车原始行判断失效原因（改数量/勾选场景，需要再查一次商品与 SKU） */
    private String invalidReasonOf(MallCartItem item) {
        MallProduct product = mallProductMapper.getById(item.getProductId());
        if (product == null) {
            return "商品已删除";
        }
        if (!Integer.valueOf(STATUS_ON_SALE).equals(product.getStatus())) {
            return "商品已下架";
        }
        MallSku sku = mallSkuMapper.getById(item.getSkuId());
        if (sku == null) {
            return "规格已失效";
        }
        if (!Integer.valueOf(STATUS_ENABLED).equals(sku.getStatus())) {
            return "该规格已停用";
        }
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        if (stock <= 0) {
            return "已售罄";
        }
        int number = item.getNumber() == null ? 0 : item.getNumber();
        if (stock < number) {
            return "库存不足（仅剩 " + stock + " 件）";
        }
        return null;
    }

    private BigDecimal subtotal(BigDecimal price, int number) {
        return price == null ? BigDecimal.ZERO : price.multiply(BigDecimal.valueOf(number));
    }
}
