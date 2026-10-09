package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 商城买家修改资料。
 *
 * <p>全部字段可空：只传想改的字段（Mapper 里用 {@code <if>} 拼 set）。
 * 头像先调 {@code POST /api/mall/user/file/avatar} 上传拿 URL，再把 URL 放进来。
 */
@Data
@Schema(description = "买家修改资料")
public class MallUserProfileDTO implements Serializable {

    @Size(max = 20, message = "昵称最长 20 个字符")
    @Schema(description = "昵称")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Size(max = 255, message = "头像地址过长")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "头像地址格式不正确")
    @Schema(description = "头像 URL；空串表示清除头像")
    private String avatar;
}
