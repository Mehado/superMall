package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分类的商品数（用于分类导航上显示「手机(23)」）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCountVO implements Serializable {

    private Long categoryId;

    @Schema(description = "该分类下在售商品数（不含后代分类）")
    private Integer total;
}
