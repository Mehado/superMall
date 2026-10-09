package com.supermall.dto;

import com.supermall.validation.EmailAccount;
import com.supermall.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "用户登录")
public class UserLoginDTO implements Serializable, CaptchaAware {

    @NotBlank(message = "邮箱不能为空")
    @EmailAccount
    @Schema(description = "登录邮箱", example = "user1@qq.com")
    private String username;

    @NotBlank(message = "密码不能为空")
    @StrongPassword
    @Schema(description = "密码", example = "Mall1234")
    private String password;

    @Schema(description = "图形验证码标识")
    private String captchaKey;

    @Schema(description = "图形验证码")
    private String captchaCode;
}
