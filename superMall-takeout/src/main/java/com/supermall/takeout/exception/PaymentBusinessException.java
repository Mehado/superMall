package com.supermall.takeout.exception;

import com.supermall.exception.BaseException;

/**
 * PaymentBusinessException
 */
public class PaymentBusinessException extends BaseException {

    public PaymentBusinessException() {
    }

    public PaymentBusinessException(String msg) {
        super(msg);
    }
}
