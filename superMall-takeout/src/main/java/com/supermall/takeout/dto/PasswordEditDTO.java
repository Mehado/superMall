package com.supermall.takeout.dto;

import com.supermall.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "修改密码")
public class PasswordEditDTO implements Serializable {

    @NotBlank(message = "原密码不能为空")
    @Schema(description = "原密码")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @StrongPassword
    @Schema(description = "新密码，需 8-32 位且同时包含大小写字母和数字", example = "Mall1234")
    private String newPassword;
}
