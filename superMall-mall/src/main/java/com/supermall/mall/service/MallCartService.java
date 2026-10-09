package com.supermall.mall.service;

import com.supermall.mall.vo.MallCartAddVO;
import com.supermall.mall.vo.MallCartVO;

/**
 * 购物车（自营，无商家维度）。
 *
 * <p>所有方法都以 {@code userId} 为参数、由 Controller 从登录态注入，
 * 不接受前端传 userId —— 否则能操作别人的购物车。
 */
public interface MallCartService {

    /** 购物车列表 + 汇总（价格实时计算，含失效商品） */
    MallCartVO get(Long userId);

    /** 购物车行数（含失效），用于角标 */
    int count(Long userId);

    /** 失效行数 */
    int invalidCount(Long userId);

    /**
     * 加入购物车。
     *
     * <p>同一 SKU 累加数量；累加结果会被库存上限截断（返回的是最终数量）。
     * 商品下架 / 规格停用 / 售罄会直接拒绝。
     */
    MallCartAddVO add(Long userId, Long skuId, Integer number);

    /** 修改数量（不能超过库存） */
    void updateNumber(Long userId, Long id, Integer number);

    /** 勾选 / 取消勾选单行；勾选失效商品会被拒绝 */
    void updateSelected(Long userId, Long id, Integer selected);

    /** 全选 / 全不选；全选只作用于有效商品 */
    void updateAllSelected(Long userId, Integer selected);

    /** 删除单行（幂等） */
    void remove(Long userId, Long id);

    /**
     * 清空购物车。
     *
     * @param scope {@code all} 清空全部；{@code invalid} 只清失效商品
     */
    void clear(Long userId, String scope);
}
