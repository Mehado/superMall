package com.supermall.takeout.service.impl;

import com.supermall.constant.JwtClaimsConstant;
import com.supermall.constant.MessageConstant;
import com.supermall.constant.RoleConstant;
import com.supermall.constant.StatusConstant;
import com.supermall.takeout.annotations.RequireCaptcha;
import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.PasswordEditDTO;
import com.supermall.dto.UserLoginDTO;
import com.supermall.dto.UserRegisterDTO;
import com.supermall.takeout.dto.UserUpdateDTO;
import com.supermall.entity.User;
import com.supermall.exception.AccountNotFoundException;
import com.supermall.exception.LoginFailedException;
import com.supermall.exception.PasswordErrorException;
import com.supermall.takeout.exception.UserNotLoginException;
import com.supermall.takeout.mapper.UserMapper;
import com.supermall.properties.JwtProperties;
import com.supermall.takeout.service.UserService;
import com.supermall.utils.JwtUtil;
import com.supermall.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    @Override
    @RequireCaptcha
    public UserLoginVO login(UserLoginDTO dto) {
        User user = userMapper.getByUsername(dto.getUsername());
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        if (StatusConstant.DISABLE.equals(user.getStatus())) {
            throw new LoginFailedException(MessageConstant.ACCOUNT_LOCKED);
        }

        return issueToken(user);
    }

    /** 签发用户端 JWT 并组装登录返回体（登录 / 注册共用） */
    private UserLoginVO issueToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        claims.put(JwtClaimsConstant.ROLE, RoleConstant.USER);
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        return UserLoginVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .token(token)
                .build();
    }

    @Override
    @Transactional
    @RequireCaptcha
    public UserLoginVO register(UserRegisterDTO dto) {
        if (userMapper.getByUsername(dto.getUsername()) != null) {
            throw new LoginFailedException(MessageConstant.USERNAME_EXISTS);
        }
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                // 昵称留空时取邮箱 @ 前面的部分（比把整串邮箱当昵称好看）
                .nickname(dto.getNickname() == null || dto.getNickname().isBlank()
                        ? localPartOf(dto.getUsername()) : dto.getNickname())
                .phone(dto.getPhone())
                .status(StatusConstant.ENABLE)
                .createTime(now)
                .updateTime(now)
                .build();
        userMapper.insert(user);

        // 注册即登录：验证码只能消费一次，不能再走一遍 login
        return issueToken(user);
    }

    /** 取邮箱 @ 前面的部分作为默认昵称 */
    private String localPartOf(String email) {
        if (email == null) {
            return "用户";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    @Override
    public User getById(Long id) {
        return userMapper.getById(id);
    }

    @Override
    public void updateProfile(UserUpdateDTO dto) {
        Long userId = requireLogin();
        User user = User.builder()
                .id(userId)
                .nickname(dto.getNickname())
                .phone(dto.getPhone())
                .avatar(dto.getAvatar())
                .build();
        userMapper.update(user);
    }

    @Override
    public void editPassword(PasswordEditDTO dto) {
        Long userId = requireLogin();
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        userMapper.update(User.builder()
                .id(userId)
                .password(passwordEncoder.encode(dto.getNewPassword()))
                .build());
    }

    private Long requireLogin() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new UserNotLoginException(MessageConstant.USER_NOT_LOGIN);
        }
        return userId;
    }
}
