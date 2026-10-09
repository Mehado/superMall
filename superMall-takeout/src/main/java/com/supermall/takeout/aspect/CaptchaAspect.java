package com.supermall.takeout.aspect;

import com.supermall.constant.MessageConstant;
import com.supermall.dto.CaptchaAware;
import com.supermall.exception.CaptchaException;
import com.supermall.takeout.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * 图形验证码校验切面。
 *
 * <p>放在切面而不是逐个 controller 里手写校验，是为了"不可能漏"：
 * 新加一个登录/注册接口，只要 DTO 实现 CaptchaAware 并标 @RequireCaptcha 就自动生效。
 *
 * <p>校验发生在业务逻辑之前，所以验证码不对时不会走到查库 / 比对密码。
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class CaptchaAspect {

    private final CaptchaService captchaService;

    @Before("@annotation(com.supermall.takeout.annotations.RequireCaptcha)")
    public void verifyCaptcha(JoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof CaptchaAware aware) {
                captchaService.verifyOrThrow(aware.getCaptchaKey(), aware.getCaptchaCode());
                return;
            }
        }
        // 标注了注解却没有 CaptchaAware 参数：属于编码错误，直接拒绝而不是放行
        log.error("@RequireCaptcha 标注的方法缺少 CaptchaAware 参数：{}", joinPoint.getSignature());
        throw new CaptchaException(MessageConstant.CAPTCHA_REQUIRED);
    }
}
