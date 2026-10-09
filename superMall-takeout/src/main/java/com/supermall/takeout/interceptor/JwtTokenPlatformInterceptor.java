package com.supermall.takeout.interceptor;

import com.supermall.constant.JwtClaimsConstant;
import com.supermall.constant.RoleConstant;
import com.supermall.context.BaseContext;
import com.supermall.properties.JwtProperties;
import com.supermall.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 平台运营（外卖）JWT 校验拦截器。
 *
 * <p>用独立的请求头 {@code platform-token} 与独立的密钥，所以商家 / 骑手 / 买家的 token
 * 打平台接口一律 401（claims 里没有 adminId，解析即失败）。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenPlatformInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String token = request.getHeader(jwtProperties.getPlatformTokenName());
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getPlatformSecretKey(), token);
            Long adminId = Long.valueOf(claims.get(JwtClaimsConstant.ADMIN_ID).toString());
            log.info("当前平台管理员id：{}", adminId);
            BaseContext.setCurrentId(adminId);
            BaseContext.setRole(RoleConstant.PLATFORM_ADMIN);
            return true;
        } catch (Exception ex) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        BaseContext.clear();
    }
}
