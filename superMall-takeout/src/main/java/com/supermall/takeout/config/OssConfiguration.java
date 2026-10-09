package com.supermall.takeout.config;

import com.supermall.properties.AliOssProperties;
import com.supermall.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OSS 客户端配置：AliOssProperties -> AliOssUtil Bean
 */
@Configuration
@Slf4j
public class OssConfiguration {

    @Bean
    public AliOssUtil aliOssUtil(AliOssProperties properties) {
        log.info("初始化 AliOssUtil：endpoint={}, bucket={}", properties.getEndpoint(), properties.getBucketName());
        return new AliOssUtil(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret(),
                properties.getBucketName());
    }
}
