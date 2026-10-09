package com.supermall.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图形验证码配置。
 */
@Component
@ConfigurationProperties(prefix = "super-mall.captcha")
@Data
public class CaptchaProperties {

    /**
     * 是否强制校验图形验证码。
     *
     * <p>默认 true（真实防机器人）。自动化脚本 / 压测可置 false 跳过，
     * 但注意：一旦关掉，三个端的登录与注册都不再校验，属于「关掉了防刷」，
     * 只应在测试环境使用。
     */
    private boolean enabled = true;

    /** 验证码有效期（秒）。同时也是 Redis key 的 TTL */
    private long ttlSeconds = 120;

    /** 字符个数 */
    private int length = 4;

    /** 图片宽度 */
    private int width = 132;

    /** 图片高度 */
    private int height = 44;

    /** Redis key 前缀 */
    private String keyPrefix = "captcha:";

    /** 邮箱额外放行的域名（逗号分隔），会追加到内置常用后缀清单 */
    private String emailExtraDomains = "";
}
