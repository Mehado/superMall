package com.supermall.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 账号必须是合法邮箱，且域名在常用邮箱后缀白名单内。
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmailAccountValidator.class)
public @interface EmailAccount {

    String message() default "账号必须是常用邮箱（如 qq.com / 163.com / gmail.com）";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
