package com.supermall.mall.enumeration;

/**
 * 订单支付状态（与订单主状态分离）。
 */
public enum MallPayStatus {
    UN_PAID("未支付"),
    PAID("已支付"),
    REFUNDED("已退款");

    private final String desc;

    MallPayStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
