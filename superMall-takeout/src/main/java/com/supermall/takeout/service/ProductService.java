package com.supermall.takeout.service;

import com.supermall.takeout.dto.ProductDTO;
import com.supermall.takeout.dto.ProductPageQueryDTO;
import com.supermall.takeout.dto.ProductQueryDTO;
import com.supermall.result.PageResult;
import com.supermall.takeout.vo.ProductVO;

import java.util.List;

public interface ProductService {

    void save(ProductDTO dto);

    void update(ProductDTO dto);

    void delete(Long id);

    /** 商家端：编辑回显（带 SKU） */
    ProductVO getForMerchant(Long id);

    /** 用户端：商品详情（带缓存：穿透 / 击穿 / 雪崩） */
    ProductVO getDetail(Long id);

    /** 商家端分页 */
    PageResult pageQuery(ProductPageQueryDTO dto);

    /** 用户端按商家分页浏览 */
    PageResult pageQueryForUser(ProductQueryDTO dto);

    /** 首页商品流：从在售商品里随机抽取 limit 条 */
    List<ProductVO> randomFeed(String keyword, Integer businessType, int limit);
}
