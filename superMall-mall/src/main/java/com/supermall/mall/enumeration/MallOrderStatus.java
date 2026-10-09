package com.supermall.mall.enumeration;

/**
 * 电商订单状态。
 *
 * <pre>
 * PENDING_PAYMENT ──支付──► PENDING_DELIVERY ──发货──► PENDING_RECEIPT ──收货──► COMPLETED
 *       │                                                                          │
 *       └──取消/超时──► CANCELLED                                                  │
 *                                                                                  │
 * 任意已支付状态（待发货/待收货/已完成）──申请退款──► REFUNDING ──审核通过──► REFUNDED
 *                                                        └──审核驳回──► 回到申请前的状态
 * </pre>
 *
 * <p>所有流转都用「条件更新」（{@code where status = 期望值}），天然防并发重复流转。
 * 电商是自营，没有商家参与，所以「商家审核」变成「运营后台审核」。
 */
public enum MallOrderStatus {

    PENDING_PAYMENT("待付款"),
    PENDING_DELIVERY("待发货"),
    PENDING_RECEIPT("待收货"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    REFUNDING("退款中"),
    REFUNDED("已退款");

    private final String desc;

    MallOrderStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /** 只有待付款可以取消（已支付的要走退款流程） */
    public static boolean isCancellable(String status) {
        return PENDING_PAYMENT.name().equals(status);
    }

    /** 已支付但还没退款的状态，才能申请退款 */
    public static boolean isRefundable(String status) {
        return PENDING_DELIVERY.name().equals(status)
                || PENDING_RECEIPT.name().equals(status)
                || COMPLETED.name().equals(status);
    }

    /** 状态中文描述；未知状态返回原值 */
    public static String descOf(String status) {
        if (status == null) {
            return null;
        }
        for (MallOrderStatus s : values()) {
            if (s.name().equals(status)) {
                return s.desc;
            }
        }
        return status;
    }
}
