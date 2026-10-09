package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品图片（mall_product_image）—— 一个 SPU 多张图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallProductImage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    private String url;

    private Integer sort;

    private LocalDateTime createTime;
}
