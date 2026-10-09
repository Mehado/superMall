package com.supermall.takeout.exception;

import com.supermall.exception.BaseException;

/**
 * RateLimitException
 */
public class RateLimitException extends BaseException {

    public RateLimitException() {
    }

    public RateLimitException(String msg) {
        super(msg);
    }
}
