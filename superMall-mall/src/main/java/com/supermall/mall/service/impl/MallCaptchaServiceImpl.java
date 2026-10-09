package com.supermall.mall.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.exception.CaptchaException;
import com.supermall.mall.constant.MallRedisKeyConstant;
import com.supermall.mall.service.MallCaptchaService;
import com.supermall.mall.utils.MallCaptchaImageUtil;
import com.supermall.properties.CaptchaProperties;
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
 * 电商板块图形验证码实现。
 *
 * <p>答案只存 Redis（{@code mall:captcha:{key}}）、带 TTL、不落库、不进日志、不下发。
 * 「取值 + 删除」用 Lua 原子完成（本项目 Redis 是 5.0.14，没有 GETDEL）。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallCaptchaServiceImpl implements MallCaptchaService {

    private final StringRedisTemplate stringRedisTemplate;
    private final CaptchaProperties captchaProperties;
    private final DefaultRedisScript<String> mallConsumeCaptchaScript;

    @Override
    public CaptchaVO generate() {
        String code = MallCaptchaImageUtil.randomCode(captchaProperties.getLength());
        String key = UUID.randomUUID().toString().replace("-", "");

        stringRedisTemplate.opsForValue().set(
                redisKey(key), code, Duration.ofSeconds(captchaProperties.getTtlSeconds()));

        String image = MallCaptchaImageUtil.renderBase64(
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

        String expected = stringRedisTemplate.execute(
                mallConsumeCaptchaScript, Collections.singletonList(redisKey(key)));
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
        return MallRedisKeyConstant.CAPTCHA_PREFIX + key;
    }
}
