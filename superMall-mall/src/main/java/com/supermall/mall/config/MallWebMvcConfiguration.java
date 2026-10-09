package com.supermall.mall.config;

import com.supermall.json.JacksonObjectMapper;
import com.supermall.mall.interceptor.MallAdminTokenInterceptor;
import com.supermall.mall.interceptor.MallTraceIdInterceptor;
import com.supermall.mall.interceptor.MallUserTokenInterceptor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
 * 电商板块 Web MVC 配置。
 *
 * <p>路径规划（一个角色一个段，拦截器只需要两条规则，后续加接口不用改这里）：
 * <pre>
 *   /api/mall/auth/**   公开（验证码、账号密码规则）
 *   /api/mall/user/**   买家；login / register 放行，其余要请求头 token
 *   /api/mall/admin/**  运营后台；login 放行，其余要请求头 mall-admin-token
 * </pre>
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class MallWebMvcConfiguration implements WebMvcConfigurer {

    private final MallUserTokenInterceptor mallUserTokenInterceptor;
    private final MallAdminTokenInterceptor mallAdminTokenInterceptor;
    private final MallTraceIdInterceptor mallTraceIdInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("[mall] 开始注册拦截器...");

        // 买家
        registry.addInterceptor(mallUserTokenInterceptor)
                .addPathPatterns("/api/mall/user/**")
                .excludePathPatterns("/api/mall/user/login", "/api/mall/user/register");

        // 运营后台管理员
        registry.addInterceptor(mallAdminTokenInterceptor)
                .addPathPatterns("/api/mall/admin/**")
                .excludePathPatterns("/api/mall/admin/login");

        // 全链路日志（与外卖同样的 traceId 机制）
        registry.addInterceptor(mallTraceIdInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
    }

    /**
     * 统一 JSON 序列化：与外卖服务保持一致的日期格式（yyyy-MM-dd HH:mm）。
     * 不配的话 LocalDateTime 会按 Jackson 默认格式输出，前端要写两套解析。
     */
    @Bean
    public JsonMapper jsonMapper() {
        return new JacksonObjectMapper();
    }

    @Bean
    public OpenAPI mallOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("superMall 电商板块接口文档")
                        .version("1.0")
                        .description("独立服务（8081）· 买家侧 /api/mall/user/** ｜ 运营后台 /api/mall/admin/**"))
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .name("JWT")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
