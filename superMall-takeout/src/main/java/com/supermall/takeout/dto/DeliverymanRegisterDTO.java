package com.supermall.takeout.dto;

import com.supermall.dto.CaptchaAware;
import com.supermall.validation.EmailAccount;
import com.supermall.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 骑手（配送员）自助注册。
 *
 * <p>骑手是平台级角色、没有店铺，注册即启用（{@code status=1}），
 * 平台运营可在后台停用异常骑手；不需要像商家那样走入驻审核。
 */
@Data
@Schema(description = "骑手注册")
public class DeliverymanRegisterDTO implements Serializable, CaptchaAware {

    @NotBlank(message = "邮箱不能为空")
    @EmailAccount
    @Schema(description = "登录邮箱（即账号）", example = "rider3@gmail.com")
    private String username;

    @NotBlank(message = "密码不能为空")
    @StrongPassword
    @Schema(description = "登录密码", example = "Mall1234")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名最长 50 个字符")
    @Schema(description = "骑手姓名 / 昵称", example = "疾风")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13700000003")
    private String phone;

    @Schema(description = "图形验证码标识")
    private String captchaKey;

    @Schema(description = "图形验证码")
    private String captchaCode;
}
