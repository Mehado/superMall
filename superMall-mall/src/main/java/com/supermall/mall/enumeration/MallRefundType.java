package com.supermall.mall.enumeration;

/**
 * 退款类型。
 */
public enum MallRefundType {

    REFUND_ONLY(1, "仅退款"),
    RETURN_REFUND(2, "退货退款");

    private final int code;
    private final String desc;

    MallRefundType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        for (MallRefundType t : values()) {
            if (t.code == code) {
                return true;
            }
        }
        return false;
    }

    public static String descOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (MallRefundType t : values()) {
            if (t.code == code) {
                return t.desc;
            }
        }
        return null;
    }
}
