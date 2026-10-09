package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 发货（运营后台）。
 */
@Data
@Schema(description = "发货")
public class MallShipDTO implements Serializable {

    @NotBlank(message = "物流公司不能为空")
    @Schema(description = "物流公司，如「顺丰速运」", requiredMode = Schema.RequiredMode.REQUIRED)
    private String company;

    @NotBlank(message = "物流单号不能为空")
    @Schema(description = "物流单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String trackingNo;
}
