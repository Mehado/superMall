package com.supermall.takeout.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解：配合 RateLimitAspect 使用，按 IP + 方法维度限流。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口内最大次数 */
    int limit() default 10;

    /** 窗口秒数 */
    int window() default 60;
}
