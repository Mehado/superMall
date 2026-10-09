package com.supermall.takeout.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.context.BaseContext;
import com.supermall.takeout.entity.Product;
import com.supermall.takeout.entity.Sku;
import com.supermall.takeout.exception.ProductBusinessException;
import com.supermall.takeout.mapper.ProductMapper;
import com.supermall.takeout.mapper.SkuMapper;
import com.supermall.takeout.service.SkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SkuServiceImpl implements SkuService {

    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;

    @Override
    public List<Sku> listByProductId(Long productId) {
        Product product = productMapper.getById(productId);
        if (product == null || !product.getMerchantId().equals(BaseContext.getCurrentId())) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        return skuMapper.listByProductId(productId);
    }

    @Override
    public Sku getById(Long id) {
        return skuMapper.getById(id);
    }
}
