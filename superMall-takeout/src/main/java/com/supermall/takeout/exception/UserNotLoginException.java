package com.supermall.takeout.exception;

import com.supermall.exception.BaseException;

/**
 * UserNotLoginException
 */
public class UserNotLoginException extends BaseException {

    public UserNotLoginException() {
    }

    public UserNotLoginException(String msg) {
        super(msg);
    }
}
