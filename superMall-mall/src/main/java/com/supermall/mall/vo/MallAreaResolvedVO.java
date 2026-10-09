package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 行政区划解析结果：由末级 {@code areaCode} 反查出的「省 / 市 / 区」名称，
 * 用于回填 {@code address} 表的三个冗余列。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "行政区划解析结果")
public class MallAreaResolvedVO implements Serializable {

    @Schema(description = "末级区划编码")
    private String areaCode;

    private String province;

    private String city;

    /** 末级为「市」时（如港澳）可能为空 */
    private String district;

    @Schema(description = "拼接后的全称，如 北京市 北京市 东城区")
    private String fullName;
}
