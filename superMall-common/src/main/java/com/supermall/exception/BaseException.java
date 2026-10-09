package com.supermall.exception;

/**
 * 业务异常基类：GlobalExceptionHandler 统一捕获并转成 Result.error
 */
public class BaseException extends RuntimeException {

    public BaseException() {
    }

    public BaseException(String msg) {
        super(msg);
    }
}
