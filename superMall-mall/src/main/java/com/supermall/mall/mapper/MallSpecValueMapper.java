package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallSpecValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 规格值（mall_spec_value）—— 白 / 黑 / M / L。
 */
@Mapper
public interface MallSpecValueMapper {

    List<MallSpecValue> listBySpecIds(@Param("specIds") List<Long> specIds);

    /** 单条插入，靠 useGeneratedKeys 回填 id（建 sku_spec 关联要用） */
    void insert(MallSpecValue specValue);

    void deleteBySpecIds(@Param("specIds") List<Long> specIds);
}
