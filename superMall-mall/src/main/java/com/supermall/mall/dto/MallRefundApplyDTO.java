package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 申请退款（买家）。
 */
@Data
@Schema(description = "申请退款")
public class MallRefundApplyDTO implements Serializable {

    @NotNull(message = "请选择退款类型")
    @Schema(description = "1 仅退款；2 退货退款", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer type;

    @NotBlank(message = "退款原因不能为空")
    @Schema(description = "退款原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @Schema(description = "补充说明")
    private String description;
}
