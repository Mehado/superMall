package com.supermall.takeout.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Lua 脚本配置类：统一把 src/main/resources/scripts/ 下的 .lua 文件加载成 Bean。
 * <p>
 * 好处：脚本独立成文件、可读性好；启动时 fail-fast；各业务类构造器注入，依赖清晰。
 */
@Configuration
@Slf4j
public class RedisScriptConfig {

    /** 库存预扣：检查 + 扣减原子执行，返回 1=成功 -1=不足 -2=未预热 */
    @Bean
    public DefaultRedisScript<Long> deductStockScript() {
        return loadScript("scripts/deduct_stock.lua");
    }

    /** 库存回补：key 存在才加回 */
    @Bean
    public DefaultRedisScript<Long> releaseStockScript() {
        return loadScript("scripts/release_stock.lua");
    }

    /** 幂等令牌消费：检查 + 删除原子执行 */
    @Bean
    public DefaultRedisScript<Long> idempotentTokenScript() {
        return loadScript("scripts/consume_idempotent_token.lua");
    }

    /** 分布式锁释放：值匹配才删除，防止误删别人的锁 */
    @Bean
    public DefaultRedisScript<Long> unlockScript() {
        return loadScript("scripts/unlock.lua");
    }

    /** 接口限流：INCR + 首次设过期 + 超限判断 */
    @Bean
    public DefaultRedisScript<Long> rateLimitScript() {
        return loadScript("scripts/rate_limit.lua");
    }

    /** 配送员抢单闸门：同一配送单只放行一个配送员 */
    @Bean
    public DefaultRedisScript<Long> grabDeliveryScript() {
        return loadScript("scripts/grab_delivery_order.lua");
    }

    /**
     * 图形验证码消费：取值 + 删除原子执行，返回答案字符串（已过期/已用过返回 null）。
     * 返回值是字符串而非数字，所以这里单独构造而不能复用 loadScript。
     */
    @Bean
    public DefaultRedisScript<String> consumeCaptchaScript() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        String path = "scripts/consume_captcha.lua";
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            script.setScriptText(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            log.info("加载 Lua 脚本：{}", path);
        } catch (IOException e) {
            throw new IllegalStateException("加载 Lua 脚本失败: " + path, e);
        }
        return script;
    }

    private DefaultRedisScript<Long> loadScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            script.setScriptText(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            log.info("加载 Lua 脚本：{}", path);
        } catch (IOException e) {
            throw new IllegalStateException("加载 Lua 脚本失败: " + path, e);
        }
        return script;
    }
}
