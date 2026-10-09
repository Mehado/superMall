package com.supermall.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * 密码强度规则（注册 / 改密 / 登录预检共用一处定义）。
 *
 * <p>规则：长度 8-32，且必须同时包含大写字母、小写字母、数字；不允许空白字符。
 * 特殊字符不强制，但计入长度。
 *
 * <p>前端通过 {@code GET /api/auth/policy} 拿到同一份规则的文字描述，
 * 避免前后端规则各写一套、改一边忘一边。
 */
public final class PasswordRule {

    private PasswordRule() {
    }

    public static final int MIN = 8;
    public static final int MAX = 32;

    /** 一句话描述（前端直接展示） */
    public static final String DESC = "8-32 位，需同时包含大写字母、小写字母和数字";

    /**
     * 返回未满足的规则项；全部满足则返回空列表。
     */
    public static List<String> violations(String value) {
        List<String> bad = new ArrayList<>();
        String v = value == null ? "" : value;

        if (v.isEmpty()) {
            bad.add("密码不能为空");
            return bad;
        }
        if (v.chars().anyMatch(Character::isWhitespace)) {
            bad.add("不能包含空格");
        }
        if (v.length() < MIN || v.length() > MAX) {
            bad.add("长度需 " + MIN + "-" + MAX + " 位");
        }
        if (v.chars().noneMatch(Character::isLowerCase)) {
            bad.add("需包含小写字母");
        }
        if (v.chars().noneMatch(Character::isUpperCase)) {
            bad.add("需包含大写字母");
        }
        if (v.chars().noneMatch(Character::isDigit)) {
            bad.add("需包含数字");
        }
        return bad;
    }

    public static boolean ok(String value) {
        return violations(value).isEmpty();
    }
}
