package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 退款审核（运营后台）。
 */
@Data
@Schema(description = "退款审核")
public class MallRefundAuditDTO implements Serializable {

    @NotNull(message = "请选择审核结果")
    @Schema(description = "true 同意退款；false 驳回", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean approved;

    @Schema(description = "审核备注（驳回时建议填写理由）")
    private String adminRemark;
}
