package com.supermall.mall;

import com.supermall.constant.JwtClaimsConstant;
import com.supermall.mall.constant.MallRedisKeyConstant;
import com.supermall.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 电商板块轻量单元测试：不加载 Spring 容器，因此不依赖 MySQL / Redis。
 * 重点验证「隔离约定」这类靠约定维持、一旦写错就出事的东西。
 */
@DisplayName("电商板块 - 隔离约定 / JWT")
class MallApplicationTests {

    private static final String USER_KEY = "supermall-user-secret-key-0123456789abcdef";
    private static final String ADMIN_KEY = "mall-admin-secret-key-0123456789abcdef";

    @Test
    @DisplayName("管理员 token 不能用买家密钥解析（角色隔离的密码学基础）")
    void adminTokenNotParseableWithUserKey() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, 1L);
        claims.put(JwtClaimsConstant.ROLE, "MALL_ADMIN");

        String adminToken = JwtUtil.createJWT(ADMIN_KEY, 60_000, claims);

        // 用自己的密钥能解析
        Claims parsed = JwtUtil.parseJWT(ADMIN_KEY, adminToken);
        assertThat(Long.parseLong(parsed.get(JwtClaimsConstant.USER_ID).toString())).isEqualTo(1L);
        assertThat(parsed.get(JwtClaimsConstant.ROLE)).isEqualTo("MALL_ADMIN");

        // 用买家密钥解析必然失败 —— 这就是"买家 token 打管理员接口会 401"的根因
        assertThatThrownBy(() -> JwtUtil.parseJWT(USER_KEY, adminToken)).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Redis key 全部带 mall: 前缀（不与外卖的 stock:* / order:* 撞车）")
    void redisKeysAreNamespaced() {
        assertThat(MallRedisKeyConstant.CAPTCHA_PREFIX).isEqualTo("mall:captcha:");
        assertThat(MallRedisKeyConstant.SKU_STOCK_PREFIX).startsWith("mall:stock:sku:");
        assertThat(MallRedisKeyConstant.SUBMIT_TOKEN_PREFIX).startsWith("mall:order:submit:token:");
        assertThat(MallRedisKeyConstant.PAY_TOKEN_PREFIX).startsWith("mall:pay:token:");
        assertThat(MallRedisKeyConstant.RATE_LIMIT_PREFIX).startsWith("mall:rate:limit:");
        // 商品详情缓存及其互斥锁（P8 提前交付）：同样必须带 mall: 前缀，
        // 尤其锁 key —— 与外卖的 cache:lock: 重名会导致两边互相释放对方的锁
        assertThat(MallRedisKeyConstant.PRODUCT_CACHE_PREFIX).isEqualTo("mall:product:detail:");
        assertThat(MallRedisKeyConstant.CACHE_LOCK_PREFIX).isEqualTo("mall:cache:lock:");

        // 所有常量都必须以 mall: 开头，防止以后新增时忘了加前缀
        assertThat(MallRedisKeyConstant.CAPTCHA_PREFIX)
                .isEqualTo("mall:captcha:");
    }
}
