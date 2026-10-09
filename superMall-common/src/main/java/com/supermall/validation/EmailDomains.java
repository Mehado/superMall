package com.supermall.validation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 邮箱后缀（域名）白名单 —— 账号必须是这些常用邮箱服务商的地址。
 *
 * <p>为什么用白名单而不是只校验格式：只校验格式挡不住 {@code qq.con} / {@code gmail.co}
 * 这类"看着对"的错别字，用户注册完才发现收不到邮件。白名单能直接拦住。
 *
 * <p>需要放行企业邮箱域名时，两种方式二选一：
 * <ol>
 *   <li>往下面的清单里加（一处生效，前端会自动跟着变，见 {@code GET /api/auth/policy}）；</li>
 *   <li>不改代码，用配置追加：
 *       {@code super-mall.validation.email-extra-domains=mycorp.com,mail.mycorp.cn}</li>
 * </ol>
 */
public final class EmailDomains {

    private EmailDomains() {
    }

    /** 常用邮箱服务商域名（内置清单） */
    public static final List<String> COMMON = List.of(
            // QQ / 腾讯系
            "qq.com", "vip.qq.com", "foxmail.com",
            // 网易系
            "163.com", "126.com", "yeah.net", "188.com", "vip.163.com", "vip.126.com",
            // 新浪 / 搜狐 / 中移动等国内
            "sina.com", "sina.cn", "sohu.com", "139.com", "189.cn", "21cn.com",
            "aliyun.com", "88.com", "wo.cn", "tom.com", "263.net", "china.com",
            // 微软系
            "outlook.com", "hotmail.com", "live.com", "live.cn", "msn.com",
            // 国际主流
            "gmail.com", "yahoo.com", "yahoo.com.cn", "icloud.com", "me.com",
            "aol.com", "zoho.com", "protonmail.com", "proton.me", "gmx.com",
            "yandex.com", "mail.com", "fastmail.com", "tutanota.com"
    );

    /**
     * 运行时白名单 = 内置清单 + 配置追加的域名。
     * 由 {@link EmailAccountValidator} 在实例化时初始化。
     */
    private static final Set<String> ALLOWED = new LinkedHashSet<>(COMMON);

    /** 追加额外域名（幂等，大小写不敏感） */
    public static synchronized void addExtra(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return;
        }
        for (String raw : commaSeparated.split(",")) {
            String d = raw.trim().toLowerCase();
            if (!d.isEmpty()) {
                ALLOWED.add(d);
            }
        }
    }

    public static boolean isAllowed(String domain) {
        return domain != null && ALLOWED.contains(domain.toLowerCase());
    }

    /** 当前生效清单（给 /api/auth/policy 用，让前端与后端共用一份） */
    public static List<String> allowed() {
        return List.copyOf(ALLOWED);
    }
}
