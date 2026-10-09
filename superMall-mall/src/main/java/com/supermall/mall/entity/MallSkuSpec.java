package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * SKU ↔ 规格值 关联（mall_sku_spec）。
 *
 * <p>带上 {@code specId} 而不只存 {@code specValueId}：渲染规格矩阵时要按规格项分组
 * （"颜色：白/黑，尺码：M/L"），只有 valueId 还得回表才知道它属于哪个规格项。
 *
 * <p>唯一键 {@code (sku_id, spec_id)} 保证一个 SKU 在每个规格项上只取一个值。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallSkuSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long skuId;

    private Long specId;

    private Long specValueId;
}
