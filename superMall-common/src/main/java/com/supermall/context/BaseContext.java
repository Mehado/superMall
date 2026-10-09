package com.supermall.context;

/**
 * 线程上下文：保存当前请求的登录主体信息。
 *
 * 三个端各自独立登录，currentId 的含义由 URL 前缀决定：
 * /api/user/**     -> 当前用户 id（sys_user.id）
 * /api/merchant/** -> 当前商家 id（merchant.id）
 * /api/delivery/** -> 当前配送员 id（deliveryman.id）
 * /api/admin/**    -> 管理端（本项目暂未开放）
 */
public class BaseContext {

    private static final ThreadLocal<Long> CURRENT_ID = new ThreadLocal<>();

    /** 当前登录主体角色：USER / MERCHANT / DELIVERYMAN */
    private static final ThreadLocal<String> CURRENT_ROLE = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        CURRENT_ID.set(id);
    }

    public static Long getCurrentId() {
        return CURRENT_ID.get();
    }

    public static void removeCurrentId() {
        CURRENT_ID.remove();
    }

    public static void setRole(String role) {
        CURRENT_ROLE.set(role);
    }

    public static String getRole() {
        return CURRENT_ROLE.get();
    }

    public static void removeRole() {
        CURRENT_ROLE.remove();
    }

    /** 请求结束时统一清理，防止线程池复用串数据 */
    public static void clear() {
        removeCurrentId();
        removeRole();
    }
}
