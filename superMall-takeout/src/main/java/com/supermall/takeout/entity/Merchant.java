package com.supermall.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家：一个商家对应一个账号，不设计内部员工与子角色。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String password;

    /** 店铺名称 */
    private String shopName;

    private String logo;

    private String description;

    private String phone;

    private String address;

    /** 1营业 0停业（平台状态，落库） */
    private Integer status;

    /** 商家自助营业状态 1营业中 0休息中（DB 为准，Redis 缓存） */
    private Integer businessStatus;

    /* ---------- 入驻审核（P8 平台运营） ---------- */

    /** 入驻审核状态：0 待审核 1 已通过 2 已驳回（MerchantAuditStatus） */
    private Integer auditStatus;

    /** 联系人姓名（入驻时填） */
    private String contactName;

    /** 营业执照 / 门店照 URL（入驻时上传） */
    private String licenseImage;

    /** 提交入驻申请时间 */
    private LocalDateTime applyTime;

    /** 平台审核时间 */
    private LocalDateTime auditTime;

    /** 驳回原因（auditStatus=2 时有值） */
    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
