package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 足迹项：商品卡片 + 最近浏览时间 + 浏览次数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "足迹项")
public class MallFootprintVO implements Serializable {

    private Long productId;

    @Schema(description = "最近浏览时间（列表按它倒序）")
    private LocalDateTime lastVisitTime;

    @Schema(description = "浏览次数")
    private Integer visitCount;

    @Schema(description = "商品卡片（商品已下架时 status=0，前端要标灰）")
    private MallProductVO product;
}
