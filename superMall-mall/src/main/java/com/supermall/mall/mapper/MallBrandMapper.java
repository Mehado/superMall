package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallBrand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 品牌（mall_brand）。
 */
@Mapper
public interface MallBrandMapper {

    MallBrand getById(@Param("id") Long id);

    /** 同名品牌（用于唯一性校验） */
    MallBrand getByName(@Param("name") String name);

    /** 后台列表：按名称模糊 + 状态过滤 */
    List<MallBrand> list(@Param("name") String name, @Param("status") Integer status);

    /** 前台用：只返回启用的 */
    List<MallBrand> listEnabled();

    /** 该品牌下有多少商品（删除前的引用检查） */
    int countProducts(@Param("brandId") Long brandId);

    void insert(MallBrand brand);

    void update(MallBrand brand);

    void deleteById(@Param("id") Long id);
}
