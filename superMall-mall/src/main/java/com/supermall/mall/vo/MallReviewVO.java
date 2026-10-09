package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价视图（商品详情页 + 我的评价共用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品评价")
public class MallReviewVO implements Serializable {

    private Long id;

    private Long productId;

    private Long skuId;

    private Long orderItemId;

    @Schema(description = "1-5 星")
    private Integer rating;

    private String content;

    @Schema(description = "初评晒图，按顺序")
    private List<String> images;

    @Schema(description = "追评内容；未追评为 null")
    private String followContent;

    @Schema(description = "追评晒图")
    private List<String> followImages;

    private LocalDateTime followTime;

    @Schema(description = "是否匿名")
    private Boolean anonymous;

    @Schema(description = "展示用昵称；匿名时固定为「匿名用户」")
    private String nickname;

    /* ---------- 商品快照（我的评价列表有用） ---------- */

    private String productName;

    private String productImage;

    @Schema(description = "规格文案，如「白 / M」")
    private String specText;

    private LocalDateTime createTime;
}
