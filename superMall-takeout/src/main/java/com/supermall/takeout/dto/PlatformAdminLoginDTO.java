package com.supermall.takeout.dto;

import com.supermall.dto.CaptchaAware;
import com.supermall.validation.EmailAccount;
import com.supermall.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 平台运营（外卖）登录。
 */
@Data
@Schema(description = "平台运营登录")
public class PlatformAdminLoginDTO implements Serializable, CaptchaAware {

    @NotBlank(message = "邮箱不能为空")
    @EmailAccount
    @Schema(description = "登录邮箱", example = "platform@supermall.com")
    private String username;

    @NotBlank(message = "密码不能为空")
    @StrongPassword
    @Schema(description = "密码", example = "Admin1234")
    private String password;

    @Schema(description = "图形验证码标识")
    private String captchaKey;

    @Schema(description = "图形验证码")
    private String captchaCode;
}
