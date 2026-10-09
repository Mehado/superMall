package com.supermall.takeout.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商家入驻申请结果 / 申请状态查询。
 *
 * <p>入驻不返回 token（还没过审，不能进商家端），只返回状态与提示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantApplyVO implements Serializable {

    private Long id;

    private String username;

    private String shopName;

    /** 0 待审核 1 已通过 2 已驳回 */
    private Integer auditStatus;

    private String auditStatusDesc;

    /** 驳回原因（auditStatus=2 时有值） */
    private String rejectReason;

    /** 给前端直接展示的提示文案 */
    private String message;
}
