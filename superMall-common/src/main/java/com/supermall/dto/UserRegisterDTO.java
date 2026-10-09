package com.supermall.dto;

import com.supermall.validation.EmailAccount;
import com.supermall.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "用户注册")
public class UserRegisterDTO implements Serializable, CaptchaAware {

    @NotBlank(message = "邮箱不能为空")
    @EmailAccount
    @Schema(description = "注册邮箱（即登录账号）", example = "newuser@qq.com")
    private String username;

    @NotBlank(message = "密码不能为空")
    @StrongPassword
    @Schema(description = "密码", example = "Mall1234")
    private String password;

    @Size(max = 20, message = "昵称最长 20 个字符")
    @Schema(description = "昵称，留空时取邮箱 @ 前面的部分")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "图形验证码标识")
    private String captchaKey;

    @Schema(description = "图形验证码")
    private String captchaCode;
}
