package com.supermall.mall.service;

import com.supermall.mall.dto.MallCategoryDTO;
import com.supermall.mall.entity.MallCategory;
import com.supermall.mall.vo.MallCategoryTreeVO;

import java.util.List;

/**
 * 商品分类（多级，最多 3 级）。
 */
public interface MallCategoryService {

    /** 分类树（status 传 null 表示含禁用的，后台用；传 1 表示只看启用的，前台用） */
    List<MallCategoryTreeVO> tree(Integer status);

    /**
     * 分类树 + 各分类的在售商品数（含后代累加），前台分类导航用。
     *
     * <p>父节点的数量 = 自己的商品数 + 所有后代的数量，所以点「服饰鞋包」
     * 显示的就是它下面所有子分类的在售商品总数。
     */
    List<MallCategoryTreeVO> treeWithProductCount();

    /** 某个分类自身 + 全部后代分类的 id（商品按父分类筛选时用） */
    List<Long> selfAndDescendantIds(Long id);

    MallCategory getById(Long id);

    /** 新增/修改；返回分类 id */
    Long save(MallCategoryDTO dto);

    /** 删除；有子分类或还有商品时拒绝 */
    void remove(Long id);
}
