package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 运营后台管理员登录返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员登录返回")
public class MallAdminLoginVO implements Serializable {

    private Long id;

    private String username;

    /** 显示名 */
    private String name;

    /** 管理员 JWT（请求头 mall-admin-token） */
    private String token;
}
