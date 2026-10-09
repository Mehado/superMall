package com.supermall.mall.interceptor;

import com.supermall.constant.JwtClaimsConstant;
import com.supermall.context.BaseContext;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.properties.MallJwtProperties;
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
 * 电商板块【运营后台管理员】JWT 拦截器。
 *
 * <p>用自己的密钥 + 自己的头名（{@code mall-admin-token}），
 * 与外卖的三个端、以及本板块的买家 token 完全隔离 ——
 * 买家的 token 拿到这里解析会因为密钥不同直接失败。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MallAdminTokenInterceptor implements HandlerInterceptor {

    private final MallJwtProperties mallJwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String token = request.getHeader(mallJwtProperties.getAdminTokenName());
        try {
            Claims claims = JwtUtil.parseJWT(mallJwtProperties.getAdminSecretKey(), token);
            Long adminId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("[mall] 当前管理员 id：{}", adminId);
            BaseContext.setCurrentId(adminId);
            BaseContext.setRole(MallMessageConstant.ADMIN);
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
