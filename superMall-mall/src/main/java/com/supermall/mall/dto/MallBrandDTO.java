package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "品牌新增/修改")
public class MallBrandDTO implements Serializable {

    @Schema(description = "不传=新增，传=修改")
    private Long id;

    @NotBlank(message = "品牌名不能为空")
    @Size(max = 50, message = "品牌名最长 50 个字符")
    private String name;

    @Size(max = 255, message = "logo 地址过长")
    private String logo;

    @Size(max = 500, message = "品牌简介最长 500 个字符")
    private String description;

    private Integer sort;

    @Schema(description = "1 启用 0 禁用，不传默认 1")
    private Integer status;
}
