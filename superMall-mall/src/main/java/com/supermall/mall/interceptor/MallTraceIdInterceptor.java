package com.supermall.mall.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 全链路日志：请求入口生成 traceId 放入 MDC，日志自动带上。
 * 上游传了 X-Trace-Id 就复用，否则生成新的。
 */
@Component
@Slf4j
public class MallTraceIdInterceptor implements HandlerInterceptor {

    public static final String TRACE_ID = "traceId";
    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID, traceId);
        request.setAttribute(START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Object start = request.getAttribute(START_TIME);
        long cost = start instanceof Long ? System.currentTimeMillis() - (Long) start : -1;
        log.info("接口耗时：uri={}, method={}, cost={}ms, status={}",
                request.getRequestURI(), request.getMethod(), cost, response.getStatus());
        // 请求结束必须清理，否则线程池复用会串 traceId
        MDC.remove(TRACE_ID);
    }
}
