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
 * 商品评价统计（商品详情页评分区）。
 *
 * <p>{@code goodRate} 口径：4 星及以上算好评；无评价时返回 100.00（与默认 rating 5.00 一致）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品评价统计")
public class MallReviewStatsVO implements Serializable {

    @Schema(description = "平均分；无评价时为 5.00")
    private BigDecimal rating;

    @Schema(description = "评价总数")
    private Integer reviewCount;

    @Schema(description = "好评率（%），4 星及以上占比")
    private BigDecimal goodRate;

    @Schema(description = "星级分布，5 星 → 1 星")
    private List<StarCount> distribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "星级分布项")
    public static class StarCount implements Serializable {
        private Integer star;
        private Integer count;
    }
}
