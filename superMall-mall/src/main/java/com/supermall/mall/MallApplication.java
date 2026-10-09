package com.supermall.mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 电商板块启动类（独立服务，端口 8081）。
 *
 * <p>与外卖服务 {@code superMall-takeout}（8080）是【两个独立进程】：
 * 一个挂了不影响另一个，可以各自部署、各自重启。
 *
 * <p>为什么 scanBasePackages 要写 "com.supermall" 而不是默认的本包：
 * 需要把 {@code superMall-common} 里的东西也扫进来 ——
 * {@code JwtProperties} / {@code CaptchaProperties} / {@code AliOssProperties}
 * 这三个 @ConfigurationProperties，以及 pojo 里的邮箱/密码校验器。
 * 外卖服务之所以不用写，是因为它的启动类就在 com.supermall 根包下。
 */
@SpringBootApplication(scanBasePackages = "com.supermall")
@EnableScheduling
public class MallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallApplication.class, args);
    }
}
