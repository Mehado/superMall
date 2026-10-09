package com.supermall.takeout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 商城启动类。
 *
 * 放在模块根包 com.supermall 下，@SpringBootApplication 的组件扫描会覆盖
 * common / pojo / server 共用的根包 com.supermall.**，
 * 全局异常处理、配置类、拦截器、切面等才能被自动注册。
 */
@SpringBootApplication(scanBasePackages = "com.supermall")
@EnableScheduling
public class TakeoutApplication {

    public static void main(String[] args) {
        SpringApplication.run(TakeoutApplication.class, args);
    }

}
