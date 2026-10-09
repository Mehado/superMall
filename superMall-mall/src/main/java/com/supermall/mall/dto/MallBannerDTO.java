package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推荐位新增 / 修改。
 */
@Data
@Schema(description = "推荐位新增/修改")
public class MallBannerDTO implements Serializable {

    @Schema(description = "不传=新增，传=修改")
    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长 100 个字符")
    private String title;

    @NotBlank(message = "图片不能为空")
    @Size(max = 255, message = "图片地址过长")
    private String image;

    @Schema(description = "1 首页轮播 2 首页金刚区 3 分类页顶部；不传默认 1")
    private Integer position;

    @Schema(description = "1 商品 2 分类 3 外部链接 0 无跳转；不传默认 1")
    private Integer linkType;

    @Schema(description = "商品 id / 分类 id / 外链 URL")
    private String linkValue;

    private Integer sort;

    @Schema(description = "1 上线 0 下线；不传默认 1")
    private Integer status;

    @Schema(description = "生效开始，格式 yyyy-MM-dd HH:mm，不传=不限")
    private LocalDateTime startTime;

    @Schema(description = "生效结束，格式 yyyy-MM-dd HH:mm，不传=不限")
    private LocalDateTime endTime;
}
