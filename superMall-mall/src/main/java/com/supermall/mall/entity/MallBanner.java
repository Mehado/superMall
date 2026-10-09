package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 首页推荐位 / 轮播图（mall_banner）。
 *
 * <p>支持时间窗（{@code startTime}~{@code endTime}，都为 null 表示长期有效），
 * 前台只返回「已上线 + 当前在生效期内」的。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallBanner implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 标题（后台识别用） */
    private String title;

    /** 图片 URL */
    private String image;

    /** 1 首页轮播 2 首页金刚区 3 分类页顶部 */
    private Integer position;

    /** 1 商品 2 分类 3 外部链接 0 无跳转 */
    private Integer linkType;

    /** 商品 id / 分类 id / 外链 URL */
    private String linkValue;

    private Integer sort;

    /** 1 上线 0 下线 */
    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
