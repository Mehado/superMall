package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "分类新增/修改（多级，最多 3 级）")
public class MallCategoryDTO implements Serializable {

    @Schema(description = "不传=新增，传=修改")
    private Long id;

    @Schema(description = "父分类 id；0 或不传 = 顶级")
    private Long parentId;

    @NotBlank(message = "分类名不能为空")
    @Size(max = 50, message = "分类名最长 50 个字符")
    private String name;

    @Size(max = 255, message = "图标地址过长")
    private String icon;

    private Integer sort;

    @Schema(description = "1 启用 0 禁用，不传默认 1")
    private Integer status;
}
