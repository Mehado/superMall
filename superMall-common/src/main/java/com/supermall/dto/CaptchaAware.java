package com.supermall.dto;

/**
 * 携带图形验证码的请求体（登录 / 注册）。
 *
 * <p>抽出接口是为了让 CaptchaAspect 一个切面就能覆盖三个端 + 注册，
 * 新增接口只要 DTO 实现它并标 @RequireCaptcha 即可。
 */
public interface CaptchaAware {

    /** 验证码标识（服务端生成时下发，用于从 Redis 取答案） */
    String getCaptchaKey();

    /** 用户填写的验证码 */
    String getCaptchaCode();
}
