package com.supermall.takeout.websocket;

import com.supermall.constant.JwtClaimsConstant;
import com.supermall.properties.JwtProperties;
import com.supermall.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务：/ws/{token}
 * token 为用户端 JWT，建立连接后可按 userId 定向推送订单状态。
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{token}")
public class WebSocketServer {

    /** userId -> Session */
    private static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /** @ServerEndpoint 对象由容器创建，用 static 桥接 Spring Bean */
    private static JwtProperties jwtProperties;

    @Autowired
    public void setJwtProperties(JwtProperties jwtProperties) {
        WebSocketServer.jwtProperties = jwtProperties;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            session.getUserProperties().put("userId", userId);
            SESSION_MAP.put(String.valueOf(userId), session);
            log.info("用户 {} 建立 WebSocket 连接", userId);
        } catch (Exception ex) {
            log.warn("WebSocket 鉴权失败，拒绝连接");
            try {
                session.close();
            } catch (IOException e) {
                log.error("关闭连接失败", e);
            }
        }
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("收到客户端消息：{}", message);
    }

    @OnClose
    public void onClose(Session session) {
        Object userId = session.getUserProperties().get("userId");
        if (userId != null) {
            SESSION_MAP.remove(String.valueOf(userId));
            log.info("用户 {} 断开 WebSocket 连接", userId);
        }
    }

    /** 定向推送给某个用户 */
    public void sendToUser(Long userId, String message) {
        Session session = SESSION_MAP.get(String.valueOf(userId));
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                log.error("定向推送失败：userId={}", userId, e);
            }
        }
    }

    /** 群发 */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = SESSION_MAP.values();
        for (Session session : sessions) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                log.error("群发消息失败", e);
            }
        }
    }
}
