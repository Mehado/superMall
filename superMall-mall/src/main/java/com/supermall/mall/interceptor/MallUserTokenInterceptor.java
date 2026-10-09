package com.supermall.mall.interceptor;

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
 * 电商板块【买家】JWT 拦截器。
 *
 * <p>刻意复用外卖的用户密钥与请求头名（{@code token}）：
 * 这样同一个账号在外卖登录后拿到的 token 可以直接调电商接口，
 * 不需要在商城再登一次 —— 前提是两边 yml 里的
 * {@code super-mall.jwt.user-secret-key} 配的是同一个密钥。
 *
 * <p>校验通过后把买家 id 放进 BaseContext，业务层用
 * {@code BaseContext.getCurrentId()} 取，不接受前端传 userId。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MallUserTokenInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String token = request.getHeader(jwtProperties.getUserTokenName());
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("[mall] 当前买家 id：{}", userId);
            BaseContext.setCurrentId(userId);
            BaseContext.setRole(RoleConstant.USER);
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
