package com.supermall.takeout.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatisticsVO implements Serializable {

    /** 待抢单 */
    private Integer waiting;

    /** 我已接单待取餐 */
    private Integer accepted;

    /** 配送中 */
    private Integer delivering;

    /** 已送达 */
    private Integer delivered;
}
