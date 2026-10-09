package com.supermall.takeout.exception;

import com.supermall.exception.BaseException;

/**
 * PermissionDeniedException
 */
public class PermissionDeniedException extends BaseException {

    public PermissionDeniedException() {
    }

    public PermissionDeniedException(String msg) {
        super(msg);
    }
}
