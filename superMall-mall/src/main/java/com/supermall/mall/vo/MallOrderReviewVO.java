package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 「某订单的评价」页视图：告诉前端哪些明细已评、哪些还没评。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单评价情况")
public class MallOrderReviewVO implements Serializable {

    private String orderNumber;

    private String orderStatus;

    @Schema(description = "是否可评价（订单已完成）")
    private Boolean canReview;

    private List<OrderItemReviewVO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单明细的评价情况")
    public static class OrderItemReviewVO implements Serializable {
        private Long orderItemId;
        private Long productId;
        private Long skuId;
        private String productName;
        private String productImage;
        private String specText;
        @Schema(description = "该明细的评价；未评为 null")
        private MallReviewVO review;
    }
}
