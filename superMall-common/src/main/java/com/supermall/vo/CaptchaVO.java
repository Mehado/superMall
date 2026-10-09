package com.supermall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 图形验证码。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图形验证码")
public class CaptchaVO implements Serializable {

    @Schema(description = "验证码标识，提交登录/注册时原样回传")
    private String key;

    @Schema(description = "PNG 图片的 base64（不含 data: 前缀）")
    private String image;

    @Schema(description = "有效期（秒）")
    private Long ttlSeconds;
}
