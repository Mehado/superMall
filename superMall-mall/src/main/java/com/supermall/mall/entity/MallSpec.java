package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品规格项（mall_spec）—— 如「颜色」「尺码」。
 *
 * <p>与规格值是【两级结构】：一个 SPU 下有若干规格项，每个规格项下若干规格值。
 * SKU 就是各规格项各取一个值组成的组合（见 mall_sku_spec）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    /** 规格名，如 颜色 / 尺码 */
    private String name;

    private Integer sort;

    private LocalDateTime createTime;
}
