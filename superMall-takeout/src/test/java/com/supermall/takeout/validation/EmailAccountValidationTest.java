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
 * 账号（邮箱）与密码强度校验的规则测试。
 *
 * <p>走真实的 Hibernate Validator，顺便验证注解接线是否正确（不只是验证器方法本身）。
 */
@DisplayName("账号校验 - 邮箱格式 / 常用后缀")
class EmailAccountValidationTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Data
    static class Form {
        @NotBlank
        @EmailAccount
        String username;
    }

    private Set<String> messages(String username) {
        Form f = new Form();
        f.setUsername(username);
        return VALIDATOR.validate(f).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    private void ok(String username) {
        assertThat(messages(username)).as("应通过：%s", username).isEmpty();
    }

    private void rejected(String username) {
        assertThat(messages(username)).as("应被拒：%s", username).isNotEmpty();
    }

    @Test
    @DisplayName("常见邮箱服务商都放行")
    void commonProvidersAccepted() {
        ok("user1@qq.com");
        ok("user1@163.com");
        ok("user1@126.com");
        ok("user1@gmail.com");
        ok("user1@outlook.com");
        ok("user1@hotmail.com");
        ok("first.last@foxmail.com");
        ok("a+b@qq.com");
        ok("user1@139.com");
    }

    @Test
    @DisplayName("大小写与首尾空格不影响判定")
    void caseInsensitiveAndTrimmed() {
        ok("User1@QQ.COM");
        ok("  user1@qq.com  ");
    }

    @Test
    @DisplayName("不是邮箱、缺 @、缺顶级域的一律拒")
    void malformedRejected() {
        rejected("not-an-email");
        rejected("user1");
        rejected("user1@");
        rejected("user1@qq");
        rejected("@qq.com");
        rejected("user1@.com");
        rejected("user1@@qq.com");
        rejected("a..b@qq.com");
        rejected("user 1@qq.com");
        rejected("user1@qq.com.");
    }

    @Test
    @DisplayName("格式对但后缀不在常用清单里 → 拒（拦住 qq.con 这类错别字）")
    void unsupportedSuffixRejected() {
        rejected("user1@qq.con");
        rejected("user1@gmial.com");
        rejected("user1@gmai.com");
        rejected("user1@163.con");
        rejected("user1@random-domain.xyz");
    }

    @Test
    @DisplayName("配置追加的域名同样放行（同一进程内共享清单）")
    void extraDomainsAccepted() {
        rejected("boss@mycorp.com");
        EmailDomains.addExtra("mycorp.com, Mail.MyCorp.CN ");
        ok("boss@mycorp.com");
        ok("boss@mail.mycorp.cn");
    }
}
