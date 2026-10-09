package com.supermall.takeout.enumeration;

/**
 * 支付状态
 */
public enum PayStatus {
    UN_PAID("未支付"),
    PAID("已支付"),
    REFUND("已退款");

    private final String desc;

    PayStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
