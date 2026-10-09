package com.supermall.takeout;

import com.supermall.constant.JwtClaimsConstant;
import com.supermall.takeout.enumeration.OrderStatus;
import com.supermall.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 轻量单元测试：不加载 Spring 容器，因此不依赖 MySQL / Redis / RabbitMQ。
 * 需要验证容器装配时，请在本地起好中间件后另外编写 @SpringBootTest。
 */
class TakeoutApplicationTests {

    private static final String SECRET = "supermall-user-secret-key-0123456789abcdef";

    @Test
    void jwtCreateAndParse() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, 1L);
        claims.put(JwtClaimsConstant.ROLE, "USER");
        String token = JwtUtil.createJWT(SECRET, 60_000, claims);

        Claims parsed = JwtUtil.parseJWT(SECRET, token);
        assertEquals(1L, Long.parseLong(parsed.get(JwtClaimsConstant.USER_ID).toString()));
        assertEquals("USER", parsed.get(JwtClaimsConstant.ROLE));
    }

    @Test
    void orderStatusRules() {
        assertTrue(OrderStatus.isCancellable(OrderStatus.PENDING_PAYMENT.name()));
        assertTrue(OrderStatus.isCancellable(OrderStatus.PAID.name()));
        assertFalse(OrderStatus.isCancellable(OrderStatus.DELIVERING.name()));
        assertFalse(OrderStatus.isCancellable(OrderStatus.COMPLETED.name()));
    }

    @Test
    void bcryptMatchesSeedPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // sql/supermall.sql 中所有演示账号密码均为 Mall1234
        // （2026-09-18 登录加固时从 123456 迁移而来，见 sql/migration-2026-09-18-email-account.sql）
        String seedHash = "$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6";
        assertTrue(encoder.matches("Mall1234", seedHash));
        // 旧密码必须已经失效，否则说明种子数据没迁移
        assertFalse(encoder.matches("123456", seedHash));
    }

    @Test
    void adminSeedPasswordIsDifferentFromBuyerPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // sql/mall.sql 的演示管理员：admin@qq.com / Admin1234
        // 管理员权限更高，刻意与买家密码不同
        String adminHash = "$2a$10$cUuwmfegKK7qRA5bpFNn2eeBi70aSR1XgjEJjQAkPljIxzv3xu7qa";
        assertTrue(encoder.matches("Admin1234", adminHash));
        assertFalse(encoder.matches("Mall1234", adminHash));
    }
}
