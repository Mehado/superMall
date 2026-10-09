package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SKU（mall_sku）。
 */
@Mapper
public interface MallSkuMapper {

    MallSku getById(@Param("id") Long id);

    List<MallSku> listByProductId(@Param("productId") Long productId);

    /** 全部 SKU（库存预热到 Redis 用） */
    List<MallSku> listAll();

    /** 单条插入，靠 useGeneratedKeys 回填 id（建 sku_spec 关联要用） */
    void insert(MallSku sku);

    void deleteByProductId(@Param("productId") Long productId);

    /**
     * 条件扣减库存（防超卖的最后一道闸门）：{@code where id=? and stock >= ?}。
     * 返回 0 表示库存不够，事务回滚。
     */
    int deductStock(@Param("id") Long id, @Param("number") Integer number);

    /** 回补库存（取消 / 退款通过时） */
    int releaseStock(@Param("id") Long id, @Param("number") Integer number);

    /** 累加销量（delta 可为负，退款时扣回） */
    int incrementSales(@Param("id") Long id, @Param("delta") Integer delta);
}
