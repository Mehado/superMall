package com.supermall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 账号与密码规则（后端为唯一事实源，前端启动时拉一次用于本地校验与提示）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "账号与密码规则")
public class AuthPolicyVO implements Serializable {

    @Schema(description = "支持的常用邮箱后缀清单")
    private List<String> emailDomains;

    @Schema(description = "邮箱格式示例")
    private String emailExample;

    @Schema(description = "密码规则文字描述")
    private String passwordDesc;

    @Schema(description = "密码最小长度")
    private Integer passwordMin;

    @Schema(description = "密码最大长度")
    private Integer passwordMax;

    @Schema(description = "是否需要图形验证码")
    private Boolean captchaRequired;
}
