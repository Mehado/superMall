package com.supermall.takeout.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.constant.MessageConstant;
import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.CategoryDTO;
import com.supermall.takeout.dto.CategoryPageQueryDTO;
import com.supermall.takeout.entity.Category;
import com.supermall.takeout.enumeration.BusinessType;
import com.supermall.takeout.exception.CategoryBusinessException;
import com.supermall.takeout.mapper.CategoryMapper;
import com.supermall.takeout.mapper.ProductMapper;
import com.supermall.result.PageResult;
import com.supermall.takeout.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public void save(CategoryDTO dto) {
        Long merchantId = BaseContext.getCurrentId();
        requireValidBusinessType(dto.getBusinessType());
        if (categoryMapper.countByName(merchantId, dto.getBusinessType(), dto.getName(), null) > 0) {
            throw new CategoryBusinessException(MessageConstant.CATEGORY_NAME_EXISTS);
        }
        LocalDateTime now = LocalDateTime.now();
        Category category = new Category();
        BeanUtils.copyProperties(dto, category);
        category.setId(null);
        category.setMerchantId(merchantId);
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        category.setCreateTime(now);
        category.setUpdateTime(now);
        categoryMapper.insert(category);
    }

    @Override
    @Transactional
    public void update(CategoryDTO dto) {
        Long merchantId = BaseContext.getCurrentId();
        Category exist = categoryMapper.getById(dto.getId(), merchantId);
        if (exist == null) {
            throw new CategoryBusinessException(MessageConstant.CATEGORY_NOT_FOUND);
        }
        requireValidBusinessType(dto.getBusinessType());
        if (categoryMapper.countByName(merchantId, dto.getBusinessType(), dto.getName(), dto.getId()) > 0) {
            throw new CategoryBusinessException(MessageConstant.CATEGORY_NAME_EXISTS);
        }
        Category category = new Category();
        BeanUtils.copyProperties(dto, category);
        category.setMerchantId(merchantId);
        categoryMapper.update(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long merchantId = BaseContext.getCurrentId();
        Category exist = categoryMapper.getById(id, merchantId);
        if (exist == null) {
            throw new CategoryBusinessException(MessageConstant.CATEGORY_NOT_FOUND);
        }
        if (productMapper.countByCategoryId(id, merchantId) > 0) {
            throw new CategoryBusinessException(MessageConstant.CATEGORY_RELATED_BY_PRODUCT);
        }
        categoryMapper.deleteById(id, merchantId);
    }

    @Override
    public PageResult pageQuery(CategoryPageQueryDTO dto) {
        dto.setMerchantId(BaseContext.getCurrentId());
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<Category> page = categoryMapper.pageQuery(dto);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public List<Category> listByMerchant(Long merchantId) {
        return categoryMapper.listByMerchant(merchantId);
    }

    @Override
    public Category getById(Long id) {
        return categoryMapper.getById(id, BaseContext.getCurrentId());
    }

    private void requireValidBusinessType(Integer businessType) {
        if (!BusinessType.isValid(businessType)) {
            throw new CategoryBusinessException(MessageConstant.BUSINESS_TYPE_INVALID);
        }
    }
}
