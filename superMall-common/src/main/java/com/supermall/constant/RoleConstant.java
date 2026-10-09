package com.supermall.constant;

/**
 * 登录主体角色：各端各自一套 JWT，角色用于日志与兜底校验。
 * 本项目不设计商家内部员工与复杂 RBAC。
 */
public class RoleConstant {

    public static final String USER = "USER";
    public static final String MERCHANT = "MERCHANT";
    public static final String DELIVERYMAN = "DELIVERYMAN";

    /**
     * 平台运营（外卖）：负责商家入驻审核、骑手管理。
     * 独立于商家 / 骑手 / 用户，使用自己的请求头 platform-token。
     */
    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
}
