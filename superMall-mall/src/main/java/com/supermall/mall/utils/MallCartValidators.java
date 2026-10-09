package com.supermall.mall.utils;

import com.supermall.mall.vo.MallCartItemVO;

/**
 * 购物车行「是否失效」的唯一判定入口。
 *
 * <p>购物车页面（展示失效原因）与下单（拦截失效商品）必须用同一套规则，
 * 否则会出现「购物车显示有效、下单却失败」这种不一致。所以抽成一个静态工具。
 */
public final class MallCartValidators {

    private static final int STATUS_ON_SALE = 1;
    private static final int STATUS_ENABLED = 1;

    private MallCartValidators() {
    }

    /**
     * @return 失效原因；有效返回 {@code null}
     */
    public static String invalidReason(MallCartItemVO item) {
        if (item == null || item.getProductId() == null || item.getProductStatus() == null) {
            return "商品已删除";
        }
        if (!Integer.valueOf(STATUS_ON_SALE).equals(item.getProductStatus())) {
            return "商品已下架";
        }
        if (item.getSkuId() == null || item.getPrice() == null || item.getStock() == null) {
            return "规格已失效";
        }
        if (!Integer.valueOf(STATUS_ENABLED).equals(item.getSkuStatus())) {
            return "该规格已停用";
        }
        if (item.getStock() <= 0) {
            return "已售罄";
        }
        int number = item.getNumber() == null ? 0 : item.getNumber();
        if (item.getStock() < number) {
            return "库存不足（仅剩 " + item.getStock() + " 件）";
        }
        return null;
    }
}
