package com.supermall.mall.exception;

import com.supermall.exception.BaseException;

/**
 * 电商板块业务异常。
 *
 * <p>复用外卖已有的那些异常子类也行，但它们是从外卖语义长出来的
 * （AddressBusinessException / ProductBusinessException …）。
 * 电商用一个自己的通用业务异常，避免"改外卖的异常类影响电商"这种反向耦合。
 * MallGlobalExceptionHandler 把它转成 {@code Result.error(msg)}。
 */
public class MallBusinessException extends BaseException {

    public MallBusinessException(String msg) {
        super(msg);
    }
}
