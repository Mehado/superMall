package com.supermall.mall.mapper;

import com.supermall.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商城买家账号。
 *
 * <p>⚠️ 操作的是**共享表** {@code sys_user}（外卖板块也在用同一张表）。
 * 因此约定：
 * <ol>
 *   <li>不加新字段、不改现有字段语义（schema 变更会同时影响外卖）</li>
 *   <li>写操作只服务「买家自己改自己的资料」（{@link #updateProfile}），
 *       平台侧的写（加启用/禁用）刻意放在 {@code MallMemberMapper}，避免权限混在一起</li>
 * </ol>
 * 这样外卖服务完全不需要感知电商的存在。
 */
@Mapper
public interface MallUserMapper {

    User getByUsername(@Param("username") String username);

    User getById(@Param("id") Long id);

    void insert(User user);

    /** 买家改自己的资料：只更新非 null 字段（头像传空串可清除） */
    int updateProfile(User user);
}
