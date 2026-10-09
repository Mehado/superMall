package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallCartItem;
import com.supermall.mall.vo.MallCartItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车（mall_cart_item）。
 *
 * <p>所有写操作都带 {@code user_id} 条件 —— 这是防越权的最后一道闸门：
 * 即使服务层校验漏了，SQL 也不会改到别人的购物车。
 */
@Mapper
public interface MallCartItemMapper {

    /** 按 (user_id, sku_id) 取行（唯一键，最多一行） */
    MallCartItem getByUserAndSku(@Param("userId") Long userId, @Param("skuId") Long skuId);

    /** 按 id 取行 */
    MallCartItem getById(@Param("id") Long id);

    /**
     * 加购：不存在则插入，存在则累加数量。
     *
     * <p>用 {@code ON DUPLICATE KEY UPDATE} + {@code LEAST(..., stock)} 做成一条原子 SQL：
     * 并发加购同一 SKU 不会插出两行，也不会把数量加到超过库存。
     */
    int upsertAdd(@Param("userId") Long userId,
                  @Param("productId") Long productId,
                  @Param("skuId") Long skuId,
                  @Param("number") Integer number,
                  @Param("stock") Integer stock);

    /** 修改数量（仅本人） */
    int updateNumber(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("number") Integer number);

    /** 勾选 / 取消勾选单行（仅本人） */
    int updateSelected(@Param("id") Long id,
                       @Param("userId") Long userId,
                       @Param("selected") Integer selected);

    /** 全选 / 全不选：作用于本人的所有行 */
    int updateSelectedAll(@Param("userId") Long userId, @Param("selected") Integer selected);

    /**
     * 全选 / 全不选：只作用于「有效」行。
     * 有效 = 商品在售 + SKU 启用 + 有库存。
     * 一键全选绝不能把失效商品也勾上。
     */
    int updateSelectedValid(@Param("userId") Long userId, @Param("selected") Integer selected);

    /** 删除单行（仅本人） */
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    /** 批量删除（下单成功后删掉已结算的行） */
    int deleteByIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    /** 清空购物车 */
    int deleteAll(@Param("userId") Long userId);

    /** 清理失效商品（商品被删/下架、SKU 失效、售罄） */
    int deleteInvalid(@Param("userId") Long userId);

    /** 购物车行数（含失效），用于角标 */
    int countByUser(@Param("userId") Long userId);

    /** 失效行数 */
    int countInvalidByUser(@Param("userId") Long userId);

    /**
     * 购物车列表（JOIN 商品与 SKU，价格实时取）。
     *
     * <p>用 LEFT JOIN：商品或 SKU 被删掉时仍要能查出来（变成失效行），
     * 否则用户会发现购物车行莫名消失。
     */
    List<MallCartItemVO> listByUser(@Param("userId") Long userId);

    /**
     * 购物车中「已选中」的行（下单用）。
     * 不过滤商品/SKU 状态 —— 服务层需要知道哪一行失效了并给出明确提示，
     * 而不是静默少结算几件。
     */
    List<MallCartItemVO> listSelectedByUser(@Param("userId") Long userId);
}
