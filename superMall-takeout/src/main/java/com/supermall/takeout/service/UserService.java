package com.supermall.takeout.service;

import com.supermall.takeout.dto.PasswordEditDTO;
import com.supermall.dto.UserLoginDTO;
import com.supermall.dto.UserRegisterDTO;
import com.supermall.takeout.dto.UserUpdateDTO;
import com.supermall.entity.User;
import com.supermall.vo.UserLoginVO;

public interface UserService {

    UserLoginVO login(UserLoginDTO dto);

    /**
     * 注册。
     *
     * <p>返回登录信息（含 token）而不是 void：验证码是【一次性】的，
     * 注册消费掉之后再调一次登录会直接被判「验证码已过期」。
     * 所以注册成功就在这里把 token 一起发出去，前端无需再登一次。
     */
    UserLoginVO register(UserRegisterDTO dto);

    User getById(Long id);

    void updateProfile(UserUpdateDTO dto);

    void editPassword(PasswordEditDTO dto);
}
