package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SKU（mall_sku）—— 一个具体的可售单元。
 *
 * <p>⚠️ **无规格商品也存在 SKU**：就是一条不关联任何规格值的记录
 * （见 {@link MallSkuSpec} 里没有 sku_id 指向它）。
 * 这样购物车/下单/库存只有一套逻辑，不用为"无规格"写分支。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallSku implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    /** 商家编码，可空 */
    private String skuCode;

    private BigDecimal price;

    private Integer stock;

    /** 累计销量（P4 下单后累加） */
    private Integer sales;

    /** 规格图（可选，选中该 SKU 时切换主图） */
    private String image;

    /** 1 启用 0 禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
