package com.supermall.mall.config;

import com.supermall.properties.AliOssProperties;
import com.supermall.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 电商板块 OSS 客户端：AliOssProperties -> AliOssUtil Bean。
 *
 * <p>与外卖的 OssConfiguration 是两个独立的 Bean（各自在自己进程里），
 * 目录不同：外卖 {@code supermall/products/}、电商 {@code supermall/mall/}。
 */
@Configuration
@Slf4j
public class MallOssConfiguration {

    @Bean
    public AliOssUtil aliOssUtil(AliOssProperties properties) {
        log.info("[mall] 初始化 AliOssUtil：endpoint={}, bucket={}, dir={}",
                properties.getEndpoint(), properties.getBucketName(), properties.getDir());
        return new AliOssUtil(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret(),
                properties.getBucketName());
    }
}
