package com.supermall.takeout.service;

import com.supermall.takeout.entity.Sku;

import java.util.List;

public interface SkuService {

    /** 商家端：某个商品的规格列表 */
    List<Sku> listByProductId(Long productId);

    Sku getById(Long id);
}
