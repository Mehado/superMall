package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallBanner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 推荐位 / 轮播图（mall_banner）。
 */
@Mapper
public interface MallBannerMapper {

    MallBanner getById(@Param("id") Long id);

    /** 后台列表：按位置 / 状态过滤 */
    List<MallBanner> list(@Param("position") Integer position, @Param("status") Integer status);

    /**
     * 前台用：已上线 + 在生效期内（start/end 为 null 视为不限）。
     * 按 sort 升序。
     */
    List<MallBanner> listEffective(@Param("position") Integer position);

    void insert(MallBanner banner);

    void update(MallBanner banner);

    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    void deleteById(@Param("id") Long id);
}
