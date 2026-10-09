package com.supermall.takeout.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 平台运营看板（外卖）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformOverviewVO implements Serializable {

    /** 待审核入驻申请数 */
    private Integer pendingMerchants;

    /** 已通过审核的商家数 */
    private Integer approvedMerchants;

    /** 已驳回的申请数 */
    private Integer rejectedMerchants;

    /** 全部商家数 */
    private Integer totalMerchants;

    /** 可接单骑手数 */
    private Integer activeRiders;

    /** 已停用骑手数 */
    private Integer disabledRiders;

    /** 全部骑手数 */
    private Integer totalRiders;
}
