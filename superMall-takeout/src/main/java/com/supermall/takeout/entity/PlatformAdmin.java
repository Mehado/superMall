package com.supermall.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 平台运营（platform_admin）。
 *
 * <p>外卖平台的"平台方"角色：审核商家入驻、管理骑手。
 * 独立于 {@code merchant} / {@code deliveryman} / {@code sys_user}，
 * 使用独立的请求头 {@code platform-token}，与电商的 {@code mall_admin} 也是两套账号。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAdmin implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 登录邮箱 */
    private String username;

    private String password;

    private String name;

    /** 1 启用 0 禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
