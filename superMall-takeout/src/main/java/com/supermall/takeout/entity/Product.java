package com.supermall.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 商家隔离字段 */
    private Long merchantId;

    private Long categoryId;

    /** 业态：1外卖食品 2百货商品（由分类派生，服务端写入） */
    private Integer businessType;

    private String name;

    private String image;

    private String description;

    /** 基础价格（无规格时即售价） */
    private BigDecimal price;

    /** 基础库存 */
    private Integer stock;

    /** 累计销量 */
    private Integer sales;

    /** 1上架 0下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
