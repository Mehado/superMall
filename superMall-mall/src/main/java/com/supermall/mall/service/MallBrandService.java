package com.supermall.mall.service;

import com.supermall.mall.dto.MallBrandDTO;
import com.supermall.mall.entity.MallBrand;

import java.util.List;

/**
 * 品牌。
 */
public interface MallBrandService {

    /** 后台列表：名称模糊 + 状态过滤 */
    List<MallBrand> list(String name, Integer status);

    /** 前台用：只返回启用的 */
    List<MallBrand> listEnabled();

    MallBrand getById(Long id);

    /** 新增/修改（按 dto.id 是否为空区分）；返回品牌 id */
    Long save(MallBrandDTO dto);

    /** 删除；该品牌下还有商品时拒绝 */
    void remove(Long id);
}
