package com.supermall.mall.service.impl;

import com.supermall.constant.JwtClaimsConstant;
import com.supermall.constant.StatusConstant;
import com.supermall.exception.AccountNotFoundException;
import com.supermall.exception.LoginFailedException;
import com.supermall.exception.PasswordErrorException;
import com.supermall.mall.annotations.RequireCaptcha;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallAdminLoginDTO;
import com.supermall.mall.entity.MallAdmin;
import com.supermall.mall.mapper.MallAdminMapper;
import com.supermall.mall.properties.MallJwtProperties;
import com.supermall.mall.service.MallAdminService;
import com.supermall.mall.vo.MallAdminLoginVO;
import com.supermall.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 运营后台管理员登录。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallAdminServiceImpl implements MallAdminService {

    private final MallAdminMapper mallAdminMapper;
    private final PasswordEncoder passwordEncoder;
    private final MallJwtProperties mallJwtProperties;

    @Override
    @RequireCaptcha
    public MallAdminLoginVO login(MallAdminLoginDTO dto) {
        MallAdmin admin = mallAdminMapper.getByUsername(dto.getUsername());
        if (admin == null) {
            throw new AccountNotFoundException(MallMessageConstant.ADMIN_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new PasswordErrorException(com.supermall.constant.MessageConstant.PASSWORD_ERROR);
        }
        if (StatusConstant.DISABLE.equals(admin.getStatus())) {
            throw new LoginFailedException(MallMessageConstant.ADMIN_DISABLED);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, admin.getId());
        claims.put(JwtClaimsConstant.USERNAME, admin.getUsername());
        claims.put(JwtClaimsConstant.NAME, admin.getName());
        claims.put(JwtClaimsConstant.ROLE, MallMessageConstant.ADMIN);
        String token = JwtUtil.createJWT(
                mallJwtProperties.getAdminSecretKey(), mallJwtProperties.getAdminTtl(), claims);

        return MallAdminLoginVO.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .name(admin.getName())
                .token(token)
                .build();
    }

    @Override
    public MallAdmin getById(Long id) {
        MallAdmin admin = mallAdminMapper.getById(id);
        if (admin != null) {
            admin.setPassword(null);
        }
        return admin;
    }
}
