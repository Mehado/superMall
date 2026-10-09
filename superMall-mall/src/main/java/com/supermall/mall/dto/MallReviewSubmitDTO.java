package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量评价一个订单（前端在「待评价」页一次提交多件商品）。
 */
@Data
@Schema(description = "批量评价订单")
public class MallReviewSubmitDTO implements Serializable {

    @NotBlank(message = "订单号不能为空")
    private String orderNumber;

    @NotEmpty(message = "请至少评价一件商品")
    @Valid
    @Schema(description = "评价列表，每件商品一条", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<MallReviewItemDTO> reviews;
}
