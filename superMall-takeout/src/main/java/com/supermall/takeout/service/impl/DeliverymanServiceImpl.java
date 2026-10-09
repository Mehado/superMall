package com.supermall.takeout.service.impl;

import com.supermall.constant.JwtClaimsConstant;
import com.supermall.constant.MessageConstant;
import com.supermall.constant.RoleConstant;
import com.supermall.constant.StatusConstant;
import com.supermall.takeout.annotations.RequireCaptcha;
import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.DeliverymanLoginDTO;
import com.supermall.takeout.dto.DeliverymanRegisterDTO;
import com.supermall.takeout.dto.DeliverymanUpdateDTO;
import com.supermall.takeout.entity.Deliveryman;
import com.supermall.exception.AccountNotFoundException;
import com.supermall.exception.LoginFailedException;
import com.supermall.exception.PasswordErrorException;
import com.supermall.takeout.mapper.DeliverymanMapper;
import com.supermall.properties.JwtProperties;
import com.supermall.takeout.service.DeliverymanService;
import com.supermall.utils.JwtUtil;
import com.supermall.takeout.vo.DeliveryLoginVO;
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
public class DeliverymanServiceImpl implements DeliverymanService {

    private final DeliverymanMapper deliverymanMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    @Override
    @RequireCaptcha
    public DeliveryLoginVO login(DeliverymanLoginDTO dto) {
        Deliveryman deliveryman = deliverymanMapper.getByUsername(dto.getUsername());
        if (deliveryman == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getPassword(), deliveryman.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        if (StatusConstant.DISABLE.equals(deliveryman.getStatus())) {
            throw new LoginFailedException(MessageConstant.ACCOUNT_LOCKED);
        }
        return issueToken(deliveryman);
    }

    @Override
    @Transactional
    @RequireCaptcha
    public DeliveryLoginVO register(DeliverymanRegisterDTO dto) {
        if (deliverymanMapper.getByUsername(dto.getUsername()) != null) {
            throw new LoginFailedException(MessageConstant.DELIVERYMAN_USERNAME_EXISTS);
        }
        LocalDateTime now = LocalDateTime.now();
        Deliveryman deliveryman = Deliveryman.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName().trim())
                .phone(dto.getPhone())
                // 骑手是平台级角色：注册即启用，可直接接单（与商家入驻审核不同）
                .status(StatusConstant.ENABLE)
                .createTime(now)
                .updateTime(now)
                .build();
        deliverymanMapper.insert(deliveryman);
        log.info("骑手注册成功：id={}, username={}, name={}",
                deliveryman.getId(), deliveryman.getUsername(), deliveryman.getName());
        // 注册即登录：验证码只能消费一次，不能再走一遍 login
        return issueToken(deliveryman);
    }

    /** 签发配送端 JWT 并组装返回体（登录 / 注册共用） */
    private DeliveryLoginVO issueToken(Deliveryman deliveryman) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.DELIVERYMAN_ID, deliveryman.getId());
        claims.put(JwtClaimsConstant.USERNAME, deliveryman.getUsername());
        claims.put(JwtClaimsConstant.ROLE, RoleConstant.DELIVERYMAN);
        String token = JwtUtil.createJWT(jwtProperties.getDeliverySecretKey(), jwtProperties.getDeliveryTtl(), claims);
        return DeliveryLoginVO.builder()
                .id(deliveryman.getId())
                .username(deliveryman.getUsername())
                .name(deliveryman.getName())
                .token(token)
                .build();
    }

    @Override
    public Deliveryman getById(Long id) {
        Deliveryman deliveryman = deliverymanMapper.getById(id);
        if (deliveryman != null) {
            deliveryman.setPassword(null);
        }
        return deliveryman;
    }

    @Override
    public void updateProfile(DeliverymanUpdateDTO dto) {
        deliverymanMapper.update(Deliveryman.builder()
                .id(BaseContext.getCurrentId())
                .name(dto.getName())
                .phone(dto.getPhone())
                .build());
    }
}
