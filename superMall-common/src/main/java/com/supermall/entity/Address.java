package com.supermall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收货地址
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String consignee;

    private String phone;

    private String province;

    private String city;

    private String district;

    /**
     * 省市区末级编码（电商 P3 加的可空列）。
     * 外卖侧写入的地址不填它，为 null；电商侧新地址由服务端按区划数据回填。
     */
    private String areaCode;

    private String detail;

    /** 标签：家 / 公司 / 学校 */
    private String label;

    /** 1默认地址 0非默认 */
    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
