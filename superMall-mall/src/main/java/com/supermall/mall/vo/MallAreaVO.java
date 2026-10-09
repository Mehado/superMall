package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 行政区划节点（省 / 市 / 区县）。
 *
 * <p>数据源：{@code resources/mall-area.json}，静态加载到内存。
 * 前端三级联动按 {@code code} 逐级查询：先拿省（parentCode=86），
 * 再拿市，最后拿区县；{@code hasChildren=false} 的是可选中的末级。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "行政区划节点")
public class MallAreaVO implements Serializable {

    @Schema(description = "区划编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "1 省 2 市 3 区县")
    private Integer level;

    @Schema(description = "是否还有下级（false 表示可以选作地址末级）")
    private Boolean hasChildren;
}
