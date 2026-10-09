package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 单条评价（针对一个订单明细）。
 */
@Data
@Schema(description = "单条评价")
public class MallReviewItemDTO implements Serializable {

    @NotNull(message = "orderItemId 不能为空")
    @Schema(description = "订单明细 id（一个明细只能评一次）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderItemId;

    @NotNull(message = "请打分")
    @Min(value = 1, message = "评分最小 1 星")
    @Max(value = 5, message = "评分最大 5 星")
    @Schema(description = "评分 1-5", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer rating;

    @Size(max = 1000, message = "评价内容最多 1000 字")
    private String content;

    @Size(max = 9, message = "晒图最多 9 张")
    @Schema(description = "晒图 URL 列表（最多 9 张）")
    private List<String> images;

    @Schema(description = "是否匿名：true 匿名")
    private Boolean anonymous;
}
