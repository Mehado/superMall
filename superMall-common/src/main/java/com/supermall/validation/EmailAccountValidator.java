package com.supermall.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.regex.Pattern;

/**
 * 邮箱账号校验：格式 + 常用后缀白名单。
 *
 * <p>校验分两步，错误提示区分开，方便前端定位：
 * <ol>
 *   <li>格式合法（局部部分 + @ + 域名 + 顶级域）</li>
 *   <li>域名在 {@link EmailDomains} 白名单内</li>
 * </ol>
 */
public class EmailAccountValidator implements ConstraintValidator<EmailAccount, String> {

    /** 常规邮箱格式：局部部分不允许首尾点、连续点 */
    private static final Pattern FORMAT = Pattern.compile(
            "^(?=.{6,254}$)(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
                    + "@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*"
                    + "\\.[A-Za-z]{2,63}$");

    /**
     * 可选：从配置追加放行的企业邮箱域名。
     * 刻意用 setter 注入并给默认值 —— 即使校验器不是由 Spring 创建（注解被忽略），
     * 或配置项缺失（注入空串），也只会退回内置清单，不会抛异常。
     */
    @Value("${super-mall.validation.email-extra-domains:}")
    public void setExtraDomains(String extra) {
        EmailDomains.addExtra(extra);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // 空值交给 @NotBlank 报错，避免重复提示
        }
        String v = value.trim();

        if (!FORMAT.matcher(v).matches()) {
            return fail(context, "邮箱格式不正确，请填写完整邮箱，例如 user@qq.com");
        }

        String domain = v.substring(v.indexOf('@') + 1).toLowerCase();
        if (!EmailDomains.isAllowed(domain)) {
            return fail(context, "暂不支持该邮箱后缀（" + domain + "），请使用常用邮箱，如 "
                    + "qq.com / 163.com / gmail.com / outlook.com");
        }
        return true;
    }

    private boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
