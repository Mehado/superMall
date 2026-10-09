package com.supermall.takeout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class CategoryDTO implements Serializable {

    private Long id;

    @NotNull(message = "业态不能为空")
    private Integer businessType;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    @NotNull(message = "排序不能为空")
    private Integer sort;

    /** 1启用 0禁用 */
    private Integer status;
}
