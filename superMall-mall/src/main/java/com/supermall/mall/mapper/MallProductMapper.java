package com.supermall.mall.mapper;

import com.supermall.mall.dto.MallProductPageQueryDTO;
import com.supermall.mall.entity.MallProduct;
import com.supermall.mall.vo.MallProductVO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品 SPU（mall_product）。
 *
 * <p>注意 {@link #page} 返回的是 {@link MallProductVO}（带分类名/品牌名），
 * 但**不查 detail 这个 TEXT 字段** —— 列表不需要，省 IO。
 */
@Mapper
public interface MallProductMapper {

    MallProduct getById(@Param("id") Long id);

    /** 列表分页（配合 PageHelper） */
    Page<MallProductVO> page(MallProductPageQueryDTO query);

    /** 详情（含分类名/品牌名 + detail） */
    MallProductVO getDetailById(@Param("id") Long id);

    void insert(MallProduct product);

    /**
     * 全量更新可编辑字段（表单提交）。
     *
     * <p>刻意【不用】 {@code <if test="xxx != null">} 的局部更新风格：
     * 商品表单每次提交的都是完整对象，用条件拼接会导致「把副标题清空」这种操作改不掉。
     * 想清空就传空字符串（前端负责区分「没填」与「要清空」）。
     *
     * <p>派生字段 price_low / price_high / stock / sales / rating 不在这里改。
     */
    void update(MallProduct product);

    /** 只改上下架状态（单独方法，避免被全量更新误改其他字段） */
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    void deleteById(@Param("id") Long id);

    /**
     * 由 SKU 重算 SPU 上的冗余字段（price_low / price_high / stock）。
     * 只统计 status=1 的 SKU。保存 SKU 后必须调用，否则列表页价格库存是旧的。
     */
    void refreshAggregates(@Param("productId") Long productId);

    /** 累加销量（delta 可为负，退款时扣回） */
    int incrementSales(@Param("id") Long id, @Param("delta") Integer delta);

    /**
     * 由评价重算 SPU 上的评价聚合字段：rating / review_count / good_rate。
     * 新增或删除评价后调用。无评价时 rating=5.00、good_rate=100.00（与默认口径一致）。
     */
    void refreshReviewAggregates(@Param("productId") Long productId);
}
