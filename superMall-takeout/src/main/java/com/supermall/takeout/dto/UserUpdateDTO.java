package com.supermall.takeout.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateDTO implements Serializable {

    private String nickname;

    @Pattern(regexp = "^$|^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 头像 URL：先调 {@code POST /api/user/avatar} 上传拿到，再传进来保存。
     * 只接受 http(s) 链接或空串（空串 = 清除头像，前端回退到首字母占位）。
     */
    @Size(max = 255, message = "头像地址过长")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "头像地址格式不正确")
    private String avatar;
}
