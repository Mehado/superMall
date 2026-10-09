package com.supermall.takeout.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置：BCrypt 加盐慢哈希
 */
@Configuration
@Slf4j
public class PasswordEncoderConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("初始化 BCrypt 密码编辑器");
        return new BCryptPasswordEncoder();
    }
}
