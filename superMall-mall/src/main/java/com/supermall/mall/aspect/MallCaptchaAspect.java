package com.supermall.mall.aspect;

import com.supermall.constant.MessageConstant;
import com.supermall.dto.CaptchaAware;
import com.supermall.exception.CaptchaException;
import com.supermall.mall.annotations.RequireCaptcha;
import com.supermall.mall.service.MallCaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * 电商板块图形验证码校验切面。
 *
 * <p>用切面而不是在 controller 里手写校验，是为了"新加接口不会漏"：
 * 只要 DTO 实现 {@code CaptchaAware}（该接口在 pojo，两边共享）并标
 * {@code @RequireCaptcha}，校验就自动生效。
 *
 * <p>校验发生在业务逻辑之前，验证码不对时不会走到查库 / 比对密码。
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class MallCaptchaAspect {

    private final MallCaptchaService mallCaptchaService;

    @Before("@annotation(com.supermall.mall.annotations.RequireCaptcha)")
    public void verifyCaptcha(JoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof CaptchaAware aware) {
                mallCaptchaService.verifyOrThrow(aware.getCaptchaKey(), aware.getCaptchaCode());
                return;
            }
        }
        log.error("@RequireCaptcha 标注的方法缺少 CaptchaAware 参数：{}", joinPoint.getSignature());
        throw new CaptchaException(MessageConstant.CAPTCHA_REQUIRED);
    }
}
