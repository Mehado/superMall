package com.supermall.mall.service.impl;

import com.supermall.constant.JwtClaimsConstant;
import com.supermall.constant.MessageConstant;
import com.supermall.constant.RoleConstant;
import com.supermall.constant.StatusConstant;
import com.supermall.dto.UserLoginDTO;
import com.supermall.dto.UserRegisterDTO;
import com.supermall.entity.User;
import com.supermall.exception.AccountNotFoundException;
import com.supermall.exception.LoginFailedException;
import com.supermall.exception.PasswordErrorException;
import com.supermall.mall.annotations.RequireCaptcha;
import com.supermall.mall.dto.MallUserProfileDTO;
import com.supermall.mall.mapper.MallUserMapper;
import com.supermall.mall.service.MallUserService;
import com.supermall.properties.JwtProperties;
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

/**
 * 商城买家账号（读写共享表 sys_user）。
 *
 * <p>与外卖侧的 UserServiceImpl 逻辑一致，但：
 * <ul>
 *   <li>是【独立的一份代码】，外卖改自己的登录逻辑不会影响电商（反之亦然）</li>
 *   <li>用外卖同一把 user 密钥签发 token，所以两边 token 通用</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallUserServiceImpl implements MallUserService {

    private final MallUserMapper mallUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    @Override
    @RequireCaptcha
    public UserLoginVO login(UserLoginDTO dto) {
        User user = mallUserMapper.getByUsername(dto.getUsername());
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

    @Override
    @Transactional
    @RequireCaptcha
    public UserLoginVO register(UserRegisterDTO dto) {
        if (mallUserMapper.getByUsername(dto.getUsername()) != null) {
            throw new LoginFailedException(MessageConstant.USERNAME_EXISTS);
        }
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .nickname(dto.getNickname() == null || dto.getNickname().isBlank()
                        ? localPartOf(dto.getUsername()) : dto.getNickname())
                .phone(dto.getPhone())
                .status(StatusConstant.ENABLE)
                .createTime(now)
                .updateTime(now)
                .build();
        mallUserMapper.insert(user);

        // 注册即登录：验证码只能消费一次，不能再走一遍 login
        return issueToken(user);
    }

    @Override
    public User getById(Long id) {
        User user = mallUserMapper.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, MallUserProfileDTO dto) {
        // 逐字段非空更新：前端只传想改的字段即可（头像传空串 = 清除）
        mallUserMapper.updateProfile(User.builder()
                .id(userId)
                .nickname(dto.getNickname())
                .phone(dto.getPhone())
                .avatar(dto.getAvatar())
                .build());
        log.info("买家修改资料：userId={}", userId);
    }

    /** 签发买家 JWT —— 用外卖同一把 user 密钥，所以两边 token 通用 */
    private UserLoginVO issueToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        claims.put(JwtClaimsConstant.ROLE, RoleConstant.USER);
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        return UserLoginVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .token(token)
                .build();
    }

    private String localPartOf(String email) {
        if (email == null) {
            return "用户";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
