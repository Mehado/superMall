package com.supermall.takeout.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.exception.CaptchaException;
import com.supermall.properties.CaptchaProperties;
import com.supermall.takeout.service.CaptchaService;
import com.supermall.takeout.utils.CaptchaImageUtil;
import com.supermall.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 图形验证码实现：JDK 画图 + Redis 存答案。
 *
 * <p>答案不落 MySQL、不进日志 —— 只存在 Redis 且带 TTL，避免被翻库或翻日志拿到。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final StringRedisTemplate stringRedisTemplate;
    private final CaptchaProperties captchaProperties;
    private final DefaultRedisScript<String> consumeCaptchaScript;

    @Override
    public CaptchaVO generate() {
        String code = CaptchaImageUtil.randomCode(captchaProperties.getLength());
        String key = UUID.randomUUID().toString().replace("-", "");

        stringRedisTemplate.opsForValue().set(
                redisKey(key), code, Duration.ofSeconds(captchaProperties.getTtlSeconds()));

        String image = CaptchaImageUtil.renderBase64(
                code, captchaProperties.getWidth(), captchaProperties.getHeight());

        return CaptchaVO.builder()
                .key(key)
                .image(image)
                .ttlSeconds(captchaProperties.getTtlSeconds())
                .build();
    }

    @Override
    public void verifyOrThrow(String key, String code) {
        if (!captchaProperties.isEnabled()) {
            return;
        }
        if (key == null || key.isBlank() || code == null || code.isBlank()) {
            throw new CaptchaException(MessageConstant.CAPTCHA_REQUIRED);
        }

        // 取值 + 删除 交给 Lua 原子完成（不能用 GETDEL：本项目 Redis 是 5.0.x）。
        // 无论校验成功与否答案都已删除，天然防重放。
        String expected = stringRedisTemplate.execute(
                consumeCaptchaScript, Collections.singletonList(redisKey(key)));
        if (expected == null) {
            throw new CaptchaException(MessageConstant.CAPTCHA_EXPIRED);
        }
        if (!expected.equalsIgnoreCase(code.trim())) {
            throw new CaptchaException(MessageConstant.CAPTCHA_ERROR);
        }
    }

    @Override
    public boolean isEnabled() {
        return captchaProperties.isEnabled();
    }

    private String redisKey(String key) {
        return captchaProperties.getKeyPrefix() + key;
    }
}
