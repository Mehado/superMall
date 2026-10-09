package com.supermall.takeout.controller.publicapi;

import com.supermall.properties.CaptchaProperties;
import com.supermall.result.Result;
import com.supermall.takeout.service.CaptchaService;
import com.supermall.validation.EmailDomains;
import com.supermall.validation.PasswordRule;
import com.supermall.vo.AuthPolicyVO;
import com.supermall.vo.CaptchaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证辅助接口（无需登录）。
 *
 * <p>路径刻意放在 /api/auth/** —— 三个端的 JWT 拦截器只覆盖
 * /api/user/**、/api/merchant/**、/api/delivery/**，所以这里是天然公开的。
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "公开-认证辅助")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;
    private final CaptchaProperties captchaProperties;

    @GetMapping("/captcha")
    @Operation(summary = "获取图形验证码（返回 key + PNG base64）")
    public Result<CaptchaVO> captcha() {
        return Result.success(captchaService.generate());
    }

    /**
     * 账号与密码规则。后端是唯一事实源：前端拉一次用于本地校验与提示，
     * 免得前后端各写一套规则、改一边忘一边。
     */
    @GetMapping("/policy")
    @Operation(summary = "获取账号与密码规则（邮箱后缀清单 / 密码强度 / 是否需要验证码）")
    public Result<AuthPolicyVO> policy() {
        EmailDomains.addExtra(captchaProperties.getEmailExtraDomains());
        return Result.success(AuthPolicyVO.builder()
                .emailDomains(EmailDomains.allowed())
                .emailExample("user1@qq.com")
                .passwordDesc(PasswordRule.DESC)
                .passwordMin(PasswordRule.MIN)
                .passwordMax(PasswordRule.MAX)
                .captchaRequired(captchaService.isEnabled())
                .build());
    }
}
