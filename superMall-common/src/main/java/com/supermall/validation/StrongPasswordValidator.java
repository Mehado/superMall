package com.supermall.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

/**
 * 密码强度校验器：把未满足的规则拼成一句人话提示，前端不用来回猜是哪条不达标。
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // 空值交给 @NotBlank 报错
        }
        List<String> bad = PasswordRule.violations(value);
        if (bad.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                        "密码不达标：" + String.join("、", bad) + "（" + PasswordRule.DESC + "）")
                .addConstraintViolation();
        return false;
    }
}
