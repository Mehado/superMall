package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分类树节点")
public class MallCategoryTreeVO implements Serializable {

    private Long id;

    private Long parentId;

    private Integer level;

    private String name;

    private String icon;

    private Integer sort;

    private Integer status;

    private LocalDateTime createTime;

    @Builder.Default
    @Schema(description = "子分类；叶子节点为空数组")
    private List<MallCategoryTreeVO> children = new ArrayList<>();

    /**
     * 该分类（含所有后代）下的在售商品数。
     *
     * <p>只有前台目录接口（{@code /api/mall/catalog/categories}）会填充；
     * 后台的分类树为 null（后台关心的是结构，不关心数量，省一次聚合查询）。
     * 分类导航上显示「手机(23)」用的就是它。
     */
    @Schema(description = "在售商品数（含后代分类）；后台接口为 null")
    private Integer productCount;
}
