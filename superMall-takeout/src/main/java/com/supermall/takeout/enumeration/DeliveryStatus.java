package com.supermall.takeout.enumeration;

/**
 * 配送单状态：WAITING -> ACCEPTED -> DELIVERING -> DELIVERED
 */
public enum DeliveryStatus {
    WAITING("待抢单"),
    ACCEPTED("已接单/待取餐"),
    DELIVERING("配送中"),
    DELIVERED("已送达");

    private final String desc;

    DeliveryStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
