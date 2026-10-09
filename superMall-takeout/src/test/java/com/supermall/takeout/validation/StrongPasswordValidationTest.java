package com.supermall.takeout.validation;

import com.supermall.validation.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 密码强度规则测试：8-32 位 + 大写 + 小写 + 数字。
 */
@DisplayName("密码规则 - 8-32 位且含大小写字母与数字")
class StrongPasswordValidationTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Data
    static class Form {
        @NotBlank
        @StrongPassword
        String password;
    }

    private Set<String> messages(String password) {
        Form f = new Form();
        f.setPassword(password);
        return VALIDATOR.validate(f).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("满足规则的密码通过")
    void strongAccepted() {
        assertThat(messages("Mall1234")).isEmpty();
        assertThat(messages("Abcdefg1")).isEmpty();
        assertThat(messages("P@ssw0rd!")).isEmpty();
        // 32 位边界
        assertThat(messages("Aa1" + "x".repeat(29))).isEmpty();
    }

    @Test
    @DisplayName("旧的 6 位纯数字 123456 必须被拒")
    void legacyWeakRejected() {
        assertThat(messages("123456")).isNotEmpty();
    }

    @Test
    @DisplayName("缺大写 / 缺小写 / 缺数字 分别被拒")
    void missingClassesRejected() {
        assertThat(messages("mall1234")).isNotEmpty();   // 无大写
        assertThat(messages("MALL1234")).isNotEmpty();   // 无小写
        assertThat(messages("MallAbcd")).isNotEmpty();   // 无数字
    }

    @Test
    @DisplayName("长度越界被拒")
    void lengthRejected() {
        assertThat(messages("Ma1")).isNotEmpty();                       // 太短
        assertThat(messages("Aa1" + "x".repeat(30))).isNotEmpty();      // 33 位，太长
    }

    @Test
    @DisplayName("含空格被拒")
    void whitespaceRejected() {
        assertThat(messages("Mall 1234")).isNotEmpty();
        assertThat(messages("Mall1234 ")).isNotEmpty();
    }

    @Test
    @DisplayName("规则描述里能定位到具体不达标项")
    void violationDetailIsActionable() {
        Set<String> msgs = messages("abcdefgh");
        String combined = String.join(" ", msgs);
        assertThat(combined).contains("大写");
        assertThat(combined).contains("数字");

        // PasswordRule 是同一份规则的工具出口，行为必须一致
        assertThat(PasswordRule.ok("Mall1234")).isTrue();
        assertThat(PasswordRule.ok("123456")).isFalse();
        assertThat(PasswordRule.violations("mall1234"))
                .containsExactly("需包含大写字母");
    }
}
