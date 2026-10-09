package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 追评（对已发表的评价追加内容）。
 */
@Data
@Schema(description = "追评")
public class MallReviewFollowDTO implements Serializable {

    @NotBlank(message = "追评内容不能为空")
    @Size(max = 1000, message = "追评内容最多 1000 字")
    private String content;

    @Size(max = 9, message = "追评晒图最多 9 张")
    private List<String> images;
}
