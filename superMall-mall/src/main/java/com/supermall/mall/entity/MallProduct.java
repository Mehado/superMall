package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SPU（mall_product）。
 *
 * <p>⚠️ {@code priceLow / priceHigh / stock} 是**由 SKU 派生的冗余字段**，
 * 由服务端在保存商品时重算（见 MallProductServiceImpl#refreshAggregates），
 * 列表页只读这几个字段就能展示价格区间与库存，不用 JOIN mall_sku。
 * 外部（前端）不要直接改它们。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 叶子分类 id */
    private Long categoryId;

    private Long brandId;

    private String name;

    /** 卖点副标题 */
    private String subtitle;

    /** 主图（与 mall_product_image 中 sort 最小的那张一致） */
    private String mainImage;

    /** 详情长文本 */
    private String detail;

    /** 派生：MIN(sku.price) */
    private BigDecimal priceLow;

    /** 派生：MAX(sku.price) */
    private BigDecimal priceHigh;

    /** 派生：SUM(sku.stock) */
    private Integer stock;

    /** 累计销量（P4 下单后累加） */
    private Integer sales;

    /** 评分（P5 评价系统维护） */
    private BigDecimal rating;

    /** 1 上架 0 下架（新建默认下架，SKU 配好再上架） */
    private Integer status;

    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
