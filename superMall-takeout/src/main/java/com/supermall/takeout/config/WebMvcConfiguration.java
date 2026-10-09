package com.supermall.takeout.config;

import com.supermall.takeout.interceptor.JwtTokenDeliveryInterceptor;
import com.supermall.takeout.interceptor.JwtTokenMerchantInterceptor;
import com.supermall.takeout.interceptor.JwtTokenPlatformInterceptor;
import com.supermall.takeout.interceptor.JwtTokenUserInterceptor;
import com.supermall.takeout.interceptor.TraceIdInterceptor;
import com.supermall.json.JacksonObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Web MVC 配置：三个端各自注册 JWT 拦截器 + 全链路日志 + 统一 JSON 序列化。
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final JwtTokenUserInterceptor jwtTokenUserInterceptor;
    private final JwtTokenMerchantInterceptor jwtTokenMerchantInterceptor;
    private final JwtTokenDeliveryInterceptor jwtTokenDeliveryInterceptor;
    private final JwtTokenPlatformInterceptor jwtTokenPlatformInterceptor;
    private final TraceIdInterceptor traceIdInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");

        // 用户端：登录/注册放行
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/api/user/**")
                .excludePathPatterns("/api/user/login", "/api/user/register");

        // 商家端：登录、入驻申请（公开）放行
        registry.addInterceptor(jwtTokenMerchantInterceptor)
                .addPathPatterns("/api/merchant/**")
                .excludePathPatterns(
                        "/api/merchant/login",
                        "/api/merchant/apply",
                        "/api/merchant/apply/status");

        // 配送端：登录、骑手注册（公开）放行
        registry.addInterceptor(jwtTokenDeliveryInterceptor)
                .addPathPatterns("/api/delivery/**")
                .excludePathPatterns("/api/delivery/login", "/api/delivery/register");

        // 平台运营端：登录放行
        registry.addInterceptor(jwtTokenPlatformInterceptor)
                .addPathPatterns("/api/platform/**")
                .excludePathPatterns("/api/platform/login");

        // 全链路日志
        registry.addInterceptor(traceIdInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
    }

    @Bean
    public JsonMapper jsonMapper() {
        return new JacksonObjectMapper();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("superMall 在线外卖商城接口文档")
                        .version("1.0")
                        .description("多商家 / 多用户 / 配送员 在线外卖商城")
                        .contact(new Contact().name("开发团队").email("dev@supermall.com"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .name("JWT")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 认证令牌，登录后获取")));
    }
}
