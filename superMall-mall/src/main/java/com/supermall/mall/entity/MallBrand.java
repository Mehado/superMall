package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品品牌（mall_brand）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallBrand implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 品牌名，唯一 */
    private String name;

    private String logo;

    private String description;

    private Integer sort;

    /** 1 启用 0 禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
