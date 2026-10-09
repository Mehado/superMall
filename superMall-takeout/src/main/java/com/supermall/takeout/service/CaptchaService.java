package com.supermall.takeout.service;

import com.supermall.vo.CaptchaVO;

/**
 * 图形验证码：生成 + 一次性校验。
 */
public interface CaptchaService {

    /** 生成一张新验证码（答案存 Redis，TTL 到期自动失效） */
    CaptchaVO generate();

    /**
     * 校验验证码，不通过抛 CaptchaException。
     *
     * <p>语义是【一次性】：无论对错，校验时都会把 Redis 里的答案删掉，
     * 所以同一个验证码不能重放；前端失败后必须刷新图片。
     *
     * @param key  生成时下发的标识
     * @param code 用户输入
     */
    void verifyOrThrow(String key, String code);

    /** 当前是否强制校验（关闭时 verifyOrThrow 直接放行） */
    boolean isEnabled();
}
