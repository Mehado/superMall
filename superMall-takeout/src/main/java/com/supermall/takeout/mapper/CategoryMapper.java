package com.supermall.takeout.mapper;

import com.supermall.takeout.dto.CategoryPageQueryDTO;
import com.supermall.takeout.entity.Category;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {

    void insert(Category category);

    void update(Category category);

    void deleteById(@Param("id") Long id, @Param("merchantId") Long merchantId);

    Category getById(@Param("id") Long id, @Param("merchantId") Long merchantId);

    /** 商家端分页（merchantId 强制隔离） */
    Page<Category> pageQuery(CategoryPageQueryDTO dto);

    /** 用户端：某商家启用中的分类 */
    List<Category> listByMerchant(@Param("merchantId") Long merchantId);

    int countByName(@Param("merchantId") Long merchantId,
                    @Param("businessType") Integer businessType,
                    @Param("name") String name,
                    @Param("excludeId") Long excludeId);
}
