package com.supermall.takeout.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解：配合 NoRepeatSubmitAspect 使用。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoRepeatSubmit {

    /** 锁的过期时间（秒），超过自动释放，防止死锁 */
    int expire() default 5;
}
