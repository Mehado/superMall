package com.supermall.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品规格 SKU
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sku implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    private Long merchantId;

    /** 规格名，如「大份」「加辣」 */
    private String name;

    private BigDecimal price;

    private Integer stock;

    /** 1启用 0禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
