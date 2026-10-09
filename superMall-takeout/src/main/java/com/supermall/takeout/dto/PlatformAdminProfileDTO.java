package com.supermall.takeout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 平台运营修改自己的资料。
 *
 * <p>只有 `name` 一个可改字段（`platform_admin` 表还只有账号/密码/状态，
 * 账号就是登录邮箱、状态由平台内部管理）。
 */
@Data
@Schema(description = "平台运营修改资料")
public class PlatformAdminProfileDTO implements Serializable {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名最长 50 个字符")
    @Schema(description = "姓名 / 昵称")
    private String name;
}
