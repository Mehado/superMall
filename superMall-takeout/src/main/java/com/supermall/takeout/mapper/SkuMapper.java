package com.supermall.takeout.mapper;

import com.supermall.takeout.entity.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkuMapper {

    void insert(Sku sku);

    void batchInsert(@Param("list") List<Sku> list);

    void update(Sku sku);

    void deleteByProductId(@Param("productId") Long productId, @Param("merchantId") Long merchantId);

    void deleteByIds(@Param("ids") List<Long> ids, @Param("merchantId") Long merchantId);

    Sku getById(Long id);

    List<Sku> listByProductId(Long productId);

    List<Sku> listByProductIds(@Param("productIds") List<Long> productIds);

    /** 扣减 SKU 库存（条件更新，库存不足返回 0） */
    int deductStock(@Param("id") Long id, @Param("number") Integer number);

    /** 回补 SKU 库存 */
    int releaseStock(@Param("id") Long id, @Param("number") Integer number);

    List<Sku> listAll();
}
