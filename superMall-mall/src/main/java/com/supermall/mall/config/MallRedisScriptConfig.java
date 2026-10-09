package com.supermall.mall.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 电商板块 Lua 脚本配置。
 *
 * <p>与外卖的 RedisScriptConfig 是两个独立的类、独立的 Bean 名，
 * 加载的是电商自己的 {@code scripts/mall_*.lua}。
 */
@Configuration
@Slf4j
public class MallRedisScriptConfig {

    /**
     * 验证码消费：取值 + 删除原子执行，返回答案字符串（已过期/已用过返回 null）。
     * 不能用 GETDEL —— 本项目 Redis 是 5.0.14，GETDEL 要 6.2+。
     */
    @Bean
    public DefaultRedisScript<String> mallConsumeCaptchaScript() {
        return loadStringScript("scripts/mall_consume_captcha.lua");
    }

    /** 库存预扣：检查 + 扣减原子执行，返回 1=成功 -1=不足 -2=未预热 */
    @Bean
    public DefaultRedisScript<Long> mallDeductStockScript() {
        return loadLongScript("scripts/mall_deduct_stock.lua");
    }

    /** 库存回补：key 存在才加回 */
    @Bean
    public DefaultRedisScript<Long> mallReleaseStockScript() {
        return loadLongScript("scripts/mall_release_stock.lua");
    }

    /** 幂等令牌消费：检查 + 删除原子执行（下单 / 支付共用） */
    @Bean
    public DefaultRedisScript<Long> mallIdempotentTokenScript() {
        return loadLongScript("scripts/mall_consume_idempotent_token.lua");
    }

    /** 分布式锁释放：值匹配才删除，防止误删别人的锁（商品详情缓存防击穿用） */
    @Bean
    public DefaultRedisScript<Long> mallUnlockScript() {
        return loadLongScript("scripts/mall_unlock.lua");
    }

    private DefaultRedisScript<Long> loadLongScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(read(path));
        return script;
    }

    private DefaultRedisScript<String> loadStringScript(String path) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setScriptText(read(path));
        return script;
    }

    private String read(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            log.info("[mall] 加载 Lua 脚本：{}", path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("[mall] 加载 Lua 脚本失败: " + path, e);
        }
    }
}
