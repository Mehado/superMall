package com.supermall.takeout.enumeration;

/**
 * 订单主状态（与配送状态分离）。
 *
 * PENDING_PAYMENT -> PAID -> MERCHANT_ACCEPTED -> WAITING_DELIVERY -> DELIVERING -> COMPLETED
 * 异常：PENDING_PAYMENT -> CANCELLED，PAID -> CANCELLED
 */
public enum OrderStatus {

    PENDING_PAYMENT("待支付"),
    PAID("已支付/待商家接单"),
    MERCHANT_ACCEPTED("商家已接单/备餐中"),
    WAITING_DELIVERY("备餐完成/待配送"),
    DELIVERING("配送中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String desc;

    OrderStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /** 只有待支付/已支付可以被取消 */
    public static boolean isCancellable(String status) {
        return PENDING_PAYMENT.name().equals(status) || PAID.name().equals(status);
    }
}
