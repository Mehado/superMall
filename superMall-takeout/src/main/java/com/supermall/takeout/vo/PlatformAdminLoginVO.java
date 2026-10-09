package com.supermall.takeout.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 平台运营登录返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAdminLoginVO implements Serializable {

    private Long id;

    private String username;

    private String name;

    /** 请求头 platform-token */
    private String token;
}
