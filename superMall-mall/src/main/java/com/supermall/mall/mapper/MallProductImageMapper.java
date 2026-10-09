package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品多图（mall_product_image）。
 */
@Mapper
public interface MallProductImageMapper {

    List<MallProductImage> listByProductId(@Param("productId") Long productId);

    void insertBatch(@Param("list") List<MallProductImage> list);

    /** 保存商品时先清空再重建，避免做「图片增删改」的差集逻辑 */
    void deleteByProductId(@Param("productId") Long productId);
}
