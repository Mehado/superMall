package com.supermall.takeout.exception;

import com.supermall.exception.BaseException;

/**
 * RepeatSubmitException
 */
public class RepeatSubmitException extends BaseException {

    public RepeatSubmitException() {
    }

    public RepeatSubmitException(String msg) {
        super(msg);
    }
}
