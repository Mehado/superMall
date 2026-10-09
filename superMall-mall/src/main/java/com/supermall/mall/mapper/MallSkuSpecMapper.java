package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallSkuSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SKU ↔ 规格值 关联（mall_sku_spec）。
 */
@Mapper
public interface MallSkuSpecMapper {

    /** 按 sku 批量取关联（组装商品详情时用） */
    List<MallSkuSpec> listBySkuIds(@Param("skuIds") List<Long> skuIds);

    void insertBatch(@Param("list") List<MallSkuSpec> list);

    void deleteBySkuIds(@Param("skuIds") List<Long> skuIds);

    /** 删除商品时用：先按 productId 找出所有 sku 再删关联 */
    void deleteByProductId(@Param("productId") Long productId);
}
