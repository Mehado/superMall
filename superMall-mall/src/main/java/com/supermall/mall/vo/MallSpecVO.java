package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 规格项（详情用）。
 *
 * <p>刻意用**字符串**而不是 id：与 {@code MallProductSaveDTO} 的结构完全对称，
 * 这样运营后台的编辑表单可以「GET 详情 → 用户改 → PUT 原样提交」，
 * 不用在前端做 valueId ↔ value 的来回映射。买家的规格选择器也只要比对字符串。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "规格项")
public class MallSpecVO implements Serializable {

    @Schema(description = "规格名，如 颜色")
    private String name;

    @Schema(description = "规格值，按顺序，如 [白, 黑]")
    private List<String> values;
}
