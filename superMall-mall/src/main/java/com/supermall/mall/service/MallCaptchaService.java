package com.supermall.mall.service;

import com.supermall.vo.CaptchaVO;

/**
 * 图形验证码：生成 + 一次性校验（电商板块）。
 *
 * <p>与外卖板块的 CaptchaService 逻辑一致，但 Redis key 前缀是
 * {@code mall:captcha:}，两边完全独立、互不干扰。
 */
public interface MallCaptchaService {

    /** 生成一张新验证码（答案存 Redis，TTL 到期自动失效） */
    CaptchaVO generate();

    /**
     * 校验验证码，不通过抛 {@link com.supermall.exception.CaptchaException}。
     *
     * <p>一次性：无论对错，校验时都会把 Redis 里的答案删掉，同一个验证码不能重放。
     */
    void verifyOrThrow(String key, String code);

    /** 当前是否强制校验（关闭时 verifyOrThrow 直接放行） */
    boolean isEnabled();
}
