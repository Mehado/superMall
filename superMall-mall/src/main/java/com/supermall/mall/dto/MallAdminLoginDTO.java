package com.supermall.mall.dto;

import com.supermall.dto.CaptchaAware;
import com.supermall.validation.EmailAccount;
import com.supermall.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 运营后台管理员登录。
 *
 * <p>复用 pojo 里的 {@code @EmailAccount} / {@code @StrongPassword}（与买家同一套规则，
 * 靠共享 pojo 模块自然获得，不需要重新写校验器），
 * 并实现 pojo 的 {@code CaptchaAware} 以便被 MallCaptchaAspect 识别。
 */
@Data
@Schema(description = "运营后台管理员登录")
public class MallAdminLoginDTO implements Serializable, CaptchaAware {

    @NotBlank(message = "邮箱不能为空")
    @EmailAccount
    @Schema(description = "管理员登录邮箱", example = "admin@qq.com")
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
