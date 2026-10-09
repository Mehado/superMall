package com.supermall.takeout.service;

import com.supermall.takeout.dto.CategoryDTO;
import com.supermall.takeout.dto.CategoryPageQueryDTO;
import com.supermall.takeout.entity.Category;
import com.supermall.result.PageResult;

import java.util.List;

public interface CategoryService {

    void save(CategoryDTO dto);

    void update(CategoryDTO dto);

    void delete(Long id);

    PageResult pageQuery(CategoryPageQueryDTO dto);

    /** 用户端：某商家启用中的分类 */
    List<Category> listByMerchant(Long merchantId);

    Category getById(Long id);
}
