package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * SKU（详情用）。
 *
 * <p>{@code specValues} 与 {@code MallSpecVO#values} 的顺序一一对应，
 * 与保存 DTO 同构。买家选规格时按字符串匹配即可定位到唯一 SKU。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SKU")
public class MallSkuVO implements Serializable {

    private Long id;

    private String skuCode;

    private BigDecimal price;

    private Integer stock;

    private Integer sales;

    private String image;

    /** 1 启用 0 禁用 */
    private Integer status;

    @Schema(description = "规格值，按规格项顺序，如 [白, M]；无规格商品为空数组")
    private List<String> specValues;
}
