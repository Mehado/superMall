package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 规格项（mall_spec）—— 颜色 / 尺码。
 *
 * <p>保存商品时同样是「先删后建」：规格和 SKU 是一起提交的，
 * 做差集更新既复杂又容易留下孤儿数据。
 */
@Mapper
public interface MallSpecMapper {

    List<MallSpec> listByProductId(@Param("productId") Long productId);

    /**
     * 单条插入，靠 useGeneratedKeys 回填 id。
     *
     * <p>为什么不用批量：MySQL 多行 INSERT 时 JDBC 只能可靠拿到第一个自增 id，
     * 而规格值/ SKU 关联都要用规格的 id，所以这里逐条插（规格项通常只有 1~3 个）。
     */
    void insert(MallSpec spec);

    void deleteByProductId(@Param("productId") Long productId);
}
