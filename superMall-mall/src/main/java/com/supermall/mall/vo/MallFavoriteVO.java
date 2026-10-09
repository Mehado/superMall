package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏项：商品卡片 + 收藏时间。
 *
 * <p>用嵌套而不是把商品字段平铺进来：商品字段有 18 个，
 * 平铺一份就等于复制一个 MallProductVO，以后改商品字段要改两处。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "收藏项")
public class MallFavoriteVO implements Serializable {

    private Long productId;

    @Schema(description = "收藏时间")
    private LocalDateTime createTime;

    @Schema(description = "商品卡片（商品已下架时 status=0，前端要标灰）")
    private MallProductVO product;
}
