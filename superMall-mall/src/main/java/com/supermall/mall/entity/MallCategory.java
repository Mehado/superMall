package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类（mall_category）—— 多级，最多 3 级。
 *
 * <p>{@code parentId = 0} 表示顶级。商品必须挂在【叶子分类】上（服务端校验）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 父分类 id；0 表示顶级 */
    private Long parentId;

    /** 层级：1 / 2 / 3 */
    private Integer level;

    private String name;

    private String icon;

    private Integer sort;

    /** 1 启用 0 禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
