package com.supermall.mall.service;

import com.supermall.dto.UserLoginDTO;
import com.supermall.dto.UserRegisterDTO;
import com.supermall.mall.dto.MallUserProfileDTO;
import com.supermall.entity.User;
import com.supermall.vo.UserLoginVO;

/**
 * 商城买家账号。
 *
 * <p>账号数据是【共享】的：商城买家就是 {@code sys_user} 里的用户，
 * 与外卖板块同一个账号。所以这里复用 pojo 里的
 * {@code UserLoginDTO} / {@code UserRegisterDTO} / {@code UserLoginVO}，
 * 不另造一套 DTO —— 接口契约也保持一致，前端两边的表单可以共用校验逻辑。
 */
public interface MallUserService {

    /** 登录（需图形验证码） */
    UserLoginVO login(UserLoginDTO dto);

    /**
     * 注册（需图形验证码）。
     *
     * <p>返回登录信息（含 token）而不是 void：验证码是一次性的，
     * 注册消费掉之后再调登录会被判「验证码已过期」，所以注册即登录。
     */
    UserLoginVO register(UserRegisterDTO dto);

    /** 查买家资料（password 置空） */
    User getById(Long id);

    /**
     * 修改买家资料（昵称 / 手机号 / 头像）。
     *
     * <p>只更新非 null 字段；头像传空串表示清除（前端会回退到首字母占位）。
     */
    void updateProfile(Long userId, MallUserProfileDTO dto);
}
