package com.supermall.mall.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallFavoriteMapper;
import com.supermall.mall.mapper.MallProductMapper;
import com.supermall.mall.service.MallFavoriteService;
import com.supermall.mall.vo.MallProductVO;
import com.supermall.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MallFavoriteServiceImpl implements MallFavoriteService {

    private final MallFavoriteMapper mallFavoriteMapper;
    private final MallProductMapper mallProductMapper;

    @Override
    @Transactional
    public void add(Long userId, Long productId) {
        requireProduct(productId);
        // INSERT IGNORE：唯一键 (user_id, product_id) 保证幂等
        mallFavoriteMapper.insertIgnore(userId, productId);
    }

    @Override
    @Transactional
    public void remove(Long userId, Long productId) {
        mallFavoriteMapper.delete(userId, productId);
    }

    @Override
    public boolean exists(Long userId, Long productId) {
        return mallFavoriteMapper.countByUserAndProduct(userId, productId) > 0;
    }

    @Override
    public int count(Long userId) {
        return mallFavoriteMapper.countByUser(userId);
    }

    @Override
    public PageResult page(Long userId, Integer page, Integer pageSize) {
        int p = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        PageHelper.startPage(p, size);
        Page<MallProductVO> result = mallFavoriteMapper.pageProducts(userId);
        return new PageResult(result.getTotal(), result.getResult());
    }

    private void requireProduct(Long productId) {
        if (mallProductMapper.getById(productId) == null) {
            throw new MallBusinessException(MallMessageConstant.PRODUCT_NOT_FOUND);
        }
    }
}
