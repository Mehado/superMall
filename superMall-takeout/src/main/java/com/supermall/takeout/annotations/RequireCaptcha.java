package com.supermall.takeout.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要校验图形验证码的方法，配合 CaptchaAspect 使用。
 *
 * <p>被标注的方法必须有一个实现了 {@code CaptchaAware} 的参数（登录/注册 DTO），
 * 切面会从中取 captchaKey / captchaCode 去校验。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireCaptcha {
}
