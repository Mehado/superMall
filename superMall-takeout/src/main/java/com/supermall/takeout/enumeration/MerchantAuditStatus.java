package com.supermall.takeout.enumeration;

/**
 * 商家入驻审核状态。
 *
 * <p>与 {@code merchant.status}（平台营业状态）是两回事：
 * <ul>
 *   <li>{@code auditStatus}：入驻申请有没有被平台批准（0 待审 / 1 通过 / 2 驳回）；</li>
 *   <li>{@code status}：店铺现在营业还是停业（1 营业 / 0 停业）。</li>
 * </ul>
 * 只有审核通过（{@code auditStatus=1}）的商家才能登录商家端。
 */
public enum MerchantAuditStatus {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回");

    private final int code;
    private final String desc;

    MerchantAuditStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static String descOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (MerchantAuditStatus s : values()) {
            if (s.code == code) {
                return s.desc;
            }
        }
        return String.valueOf(code);
    }
}
