package com.supermall.mall.controller;

import com.supermall.mall.service.MallCaptchaService;
import com.supermall.properties.CaptchaProperties;
import com.supermall.result.Result;
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
 * 电商板块认证辅助接口（无需登录）。
 *
 * <p>路径 /api/mall/auth/** 不在任何拦截器的 addPathPatterns 里，所以天然公开。
 */
@RestController
@RequestMapping("/api/mall/auth")
@Tag(name = "电商-认证辅助")
@RequiredArgsConstructor
public class MallAuthController {

    private final MallCaptchaService mallCaptchaService;
    private final CaptchaProperties captchaProperties;

    @GetMapping("/captcha")
    @Operation(summary = "获取图形验证码（返回 key + PNG base64）")
    public Result<CaptchaVO> captcha() {
        return Result.success(mallCaptchaService.generate());
    }

    /**
     * 账号与密码规则。与外卖返回同一套规则（共享 pojo 的 EmailDomains / PasswordRule），
     * 前端电商站可以直接复用同一套本地校验。
     */
    @GetMapping("/policy")
    @Operation(summary = "获取账号与密码规则")
    public Result<AuthPolicyVO> policy() {
        EmailDomains.addExtra(captchaProperties.getEmailExtraDomains());
        return Result.success(AuthPolicyVO.builder()
                .emailDomains(EmailDomains.allowed())
                .emailExample("buyer@qq.com")
                .passwordDesc(PasswordRule.DESC)
                .passwordMin(PasswordRule.MIN)
                .passwordMax(PasswordRule.MAX)
                .captchaRequired(mallCaptchaService.isEnabled())
                .build());
    }
}
