package com.supermall.takeout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 平台运营审核商家入驻申请。
 */
@Data
@Schema(description = "商家入驻审核")
public class MerchantAuditDTO implements Serializable {

    @NotNull(message = "请选择审核结果")
    @Schema(description = "true 通过 / false 驳回", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean approved;

    @Size(max = 255, message = "驳回原因最长 255 个字符")
    @Schema(description = "驳回原因（驳回时必填）", example = "营业执照照片不清晰，请重新上传")
    private String reason;
}
