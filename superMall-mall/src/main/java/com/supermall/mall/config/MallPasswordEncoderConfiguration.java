package com.supermall.mall.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密：BCrypt 加盐慢哈希。
 *
 * <p>必须与外卖服务用同一个算法 —— 两边共享 {@code sys_user} 表，
 * 外卖写的密文电商要能校验通过（BCrypt 天然跨进程通用，只要算法一致）。
 */
@Configuration
@Slf4j
public class MallPasswordEncoderConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("[mall] 初始化 BCrypt 密码编辑器");
        return new BCryptPasswordEncoder();
    }
}
