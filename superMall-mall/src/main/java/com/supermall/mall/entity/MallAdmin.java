package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 电商板块运营后台管理员（mall_admin）。
 *
 * <p>刻意独立于 {@code sys_user}：管理员是平台内部角色，
 * 复用买家表就得给它加 role 语义，那就动了外卖共享表的结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallAdmin implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 登录邮箱 */
    private String username;

    /** BCrypt 密文 */
    private String password;

    /** 显示名 */
    private String name;

    /** 1 启用 0 禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
