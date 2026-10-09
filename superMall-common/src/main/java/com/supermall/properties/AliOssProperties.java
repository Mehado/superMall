package com.supermall.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置。
 * 敏感项（access-key-id / access-key-secret）以 jasypt ENC(...) 密文写入 yml。
 */
@Component
@ConfigurationProperties(prefix = "super-mall.alioss")
@Data
public class AliOssProperties {

    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    private String bucketName;

    /** 上传目录前缀，默认 supermall/ */
    private String dir = "supermall/";

    /**
     * 头像目录前缀（两个板块共用同一套 OSS bucket）。
     * 头像与商品图分开存，便于单独清理 / 加生命周期规则。
     */
    private String avatarDir = "supermall/avatars/";
}
