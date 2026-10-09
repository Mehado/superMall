package com.supermall.mall.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 电商板块自己的 JWT 配置（运营后台管理员）。
 *
 * <p>买家的 JWT 刻意【复用】外卖那套（{@code super-mall.jwt.user-secret-key}
 * + 请求头 {@code token}），好处是同一个账号在两个板块通用：
 * 在外卖登录拿到的 token，直接可以调电商的买家接口。
 *
 * <p>管理员是电商独有的角色，所以单独一套密钥 + 单独头名，
 * 不与外卖的三个端混用。配置项放在 {@code super-mall.mall-jwt} 下，
 * 因此不需要修改 common 里的 {@code JwtProperties}（外卖零影响）。
 */
@Component
@ConfigurationProperties(prefix = "super-mall.mall-jwt")
@Data
public class MallJwtProperties {

    /** 管理员 JWT 密钥（HS256，>=32 字节） */
    private String adminSecretKey;

    /** 管理员 token 有效期（毫秒） */
    private long adminTtl;

    /** 管理员请求头名 */
    private String adminTokenName;
}
