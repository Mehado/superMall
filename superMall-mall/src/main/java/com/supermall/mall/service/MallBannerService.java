package com.supermall.mall.service;

import com.supermall.mall.dto.MallBannerDTO;
import com.supermall.mall.entity.MallBanner;

import java.util.List;

/**
 * 首页推荐位 / 轮播图。
 */
public interface MallBannerService {

    /** 后台列表（按位置 / 状态过滤） */
    List<MallBanner> list(Integer position, Integer status);

    /**
     * 前台：已上线 + 在生效期内的推荐位，按 sort 升序。
     *
     * @param position 1 首页轮播 2 首页金刚区 3 分类页顶部；不传=全部位置
     */
    List<MallBanner> listEffective(Integer position);

    MallBanner getById(Long id);

    /** 新增/修改；返回 id */
    Long save(MallBannerDTO dto);

    /** 上/下线：1 上线 0 下线 */
    void updateStatus(Long id, Integer status);

    void remove(Long id);
}
