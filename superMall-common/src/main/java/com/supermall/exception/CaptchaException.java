package com.supermall.exception;

/**
 * 图形验证码异常：缺失、过期、错误都走这里，统一由 GlobalExceptionHandler 转 Result.error。
 */
public class CaptchaException extends BaseException {

    public CaptchaException() {
    }

    public CaptchaException(String msg) {
        super(msg);
    }
}
