package com.supermall.mall.enumeration;

/**
 * 退款申请状态。
 */
public enum MallRefundStatus {
    PENDING("待审核"),
    APPROVED("已同意"),
    REJECTED("已拒绝");

    private final String desc;

    MallRefundStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /** 状态中文描述；未知状态返回原值 */
    public static String descOf(String status) {
        if (status == null) {
            return null;
        }
        for (MallRefundStatus s : values()) {
            if (s.name().equals(status)) {
                return s.desc;
            }
        }
        return status;
    }
}
