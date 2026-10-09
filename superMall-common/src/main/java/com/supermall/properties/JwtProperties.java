package com.supermall.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置：三个端各自独立密钥与 token 头名称。
 */
@Component
@ConfigurationProperties(prefix = "super-mall.jwt")
@Data
public class JwtProperties {

    /** 用户端 */
    private String userSecretKey;
    private long userTtl;
    private String userTokenName;

    /** 商家端 */
    private String merchantSecretKey;
    private long merchantTtl;
    private String merchantTokenName;

    /** 配送端 */
    private String deliverySecretKey;
    private long deliveryTtl;
    private String deliveryTokenName;

    /** 平台运营（外卖）：审核商家入驻、管理骑手 */
    private String platformSecretKey;
    private long platformTtl;
    private String platformTokenName;
}
