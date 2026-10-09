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
 * 商家端 JWT 校验拦截器：currentId = merchantId，用于多商家数据隔离。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenMerchantInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String token = request.getHeader(jwtProperties.getMerchantTokenName());
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getMerchantSecretKey(), token);
            Long merchantId = Long.valueOf(claims.get(JwtClaimsConstant.MERCHANT_ID).toString());
            log.info("当前商家id：{}", merchantId);
            BaseContext.setCurrentId(merchantId);
            BaseContext.setRole(RoleConstant.MERCHANT);
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
