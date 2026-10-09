package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 加购结果：返回该 SKU 在购物车里的**最终数量**。
 *
 * <p>加购是累加的，且会被库存上限截断（见 {@code MallCartServiceImpl#add}）。
 * 前端拿 number 更新角标即可，不用再拉一次购物车。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "加购结果")
public class MallCartAddVO implements Serializable {

    private Long id;

    private Long skuId;

    @Schema(description = "该 SKU 在购物车里的最终数量（可能因库存上限被截断）")
    private Integer number;
}
