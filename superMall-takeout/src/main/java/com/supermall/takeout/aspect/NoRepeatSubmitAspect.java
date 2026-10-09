package com.supermall.takeout.aspect;

import com.supermall.takeout.annotations.NoRepeatSubmit;
import com.supermall.context.BaseContext;
import com.supermall.takeout.exception.RepeatSubmitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交切面：Redis setIfAbsent 做分布式锁
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class NoRepeatSubmitAspect {

    private final StringRedisTemplate stringRedisTemplate;
    /** 释放锁脚本：由 RedisScriptConfig 从 .lua 文件加载为 Bean */
    private final DefaultRedisScript<Long> unlockScript;

    @Pointcut("@annotation(noRepeatSubmit)")
    public void noRepeatPointcut(NoRepeatSubmit noRepeatSubmit) {
    }

    @Around("noRepeatPointcut(noRepeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, NoRepeatSubmit noRepeatSubmit) throws Throwable {
        // 1. 拼 key：主体 + 方法 + 参数 hash
        Long currentId = BaseContext.getCurrentId();
        String userKey = currentId == null ? "anonymous" : String.valueOf(currentId);
        String methodKey = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        int argHash = Arrays.deepHashCode(joinPoint.getArgs());
        String key = "repeat:submit:" + userKey + "." + methodKey + "." + argHash;

        // 2. 抢锁：SETNX + TTL，value 是本请求的 UUID
        String value = UUID.randomUUID().toString();
        Boolean isLock = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, noRepeatSubmit.expire(), TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(isLock)) {
            log.warn("重复提交被拦截：{}", key);
            throw new RepeatSubmitException("操作过于频繁，请勿重复提交");
        }
        try {
            return joinPoint.proceed();
        } finally {
            stringRedisTemplate.execute(unlockScript, Collections.singletonList(key), value);
        }
    }
}
