package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品分类（mall_category，多级）。
 */
@Mapper
public interface MallCategoryMapper {

    MallCategory getById(@Param("id") Long id);

    /** 同级同名（用于唯一性校验） */
    MallCategory getByParentAndName(@Param("parentId") Long parentId, @Param("name") String name);

    /** 全部分类（一次取完在内存里拼树 / 算后代，分类量小，比递归查库快且不依赖 MySQL 8+ 的 CTE） */
    List<MallCategory> listAll(@Param("status") Integer status);

    /** 直接子分类 */
    List<MallCategory> listChildren(@Param("parentId") Long parentId);

    /** 该分类（不含后代）下的商品数 */
    int countProducts(@Param("categoryId") Long categoryId);

    /**
     * 各分类的在售商品数（用于前台分类导航显示「手机(23)」）。
     * 不含后代，后代由服务层在内存里累加。
     */
    List<com.supermall.mall.vo.CategoryCountVO> countOnSaleGroupByCategory();

    void insert(MallCategory category);

    void update(MallCategory category);

    void deleteById(@Param("id") Long id);
}
