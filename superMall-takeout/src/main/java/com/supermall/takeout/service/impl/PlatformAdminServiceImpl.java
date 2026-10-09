package com.supermall.takeout.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.constant.JwtClaimsConstant;
import com.supermall.constant.MessageConstant;
import com.supermall.constant.RoleConstant;
import com.supermall.constant.StatusConstant;
import com.supermall.exception.AccountNotFoundException;
import com.supermall.exception.LoginFailedException;
import com.supermall.exception.PasswordErrorException;
import com.supermall.properties.JwtProperties;
import com.supermall.result.PageResult;
import com.supermall.takeout.annotations.RequireCaptcha;
import com.supermall.takeout.dto.MerchantAuditDTO;
import com.supermall.takeout.dto.PlatformAdminLoginDTO;
import com.supermall.takeout.dto.PlatformAdminProfileDTO;
import com.supermall.takeout.entity.Deliveryman;
import com.supermall.takeout.entity.Merchant;
import com.supermall.takeout.entity.PlatformAdmin;
import com.supermall.takeout.enumeration.MerchantAuditStatus;
import com.supermall.takeout.exception.MerchantBusinessException;
import com.supermall.takeout.mapper.DeliverymanMapper;
import com.supermall.takeout.mapper.MerchantMapper;
import com.supermall.takeout.mapper.PlatformAdminMapper;
import com.supermall.takeout.service.PlatformAdminService;
import com.supermall.takeout.vo.PlatformAdminLoginVO;
import com.supermall.takeout.vo.PlatformOverviewVO;
import com.supermall.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台运营实现。
 *
 * <p>审核用条件更新（{@code where audit_status = 0}）保证并发/重复审核只会成功一次。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlatformAdminServiceImpl implements PlatformAdminService {

    private static final int RIDER_ENABLED = 1;
    private static final int RIDER_DISABLED = 0;

    private final PlatformAdminMapper platformAdminMapper;
    private final MerchantMapper merchantMapper;
    private final DeliverymanMapper deliverymanMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    // ============================================================
    // 登录
    // ============================================================

    @Override
    @RequireCaptcha
    public PlatformAdminLoginVO login(PlatformAdminLoginDTO dto) {
        PlatformAdmin admin = platformAdminMapper.getByUsername(dto.getUsername());
        if (admin == null) {
            throw new AccountNotFoundException(MessageConstant.PLATFORM_ADMIN_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        if (StatusConstant.DISABLE.equals(admin.getStatus())) {
            throw new LoginFailedException(MessageConstant.PLATFORM_ADMIN_DISABLED);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.ADMIN_ID, admin.getId());
        claims.put(JwtClaimsConstant.USERNAME, admin.getUsername());
        claims.put(JwtClaimsConstant.ROLE, RoleConstant.PLATFORM_ADMIN);
        String token = JwtUtil.createJWT(jwtProperties.getPlatformSecretKey(),
                jwtProperties.getPlatformTtl(), claims);

        log.info("平台管理员登录：id={}, username={}", admin.getId(), admin.getUsername());
        return PlatformAdminLoginVO.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .name(admin.getName())
                .token(token)
                .build();
    }

    @Override
    public PlatformAdmin getById(Long id) {
        PlatformAdmin admin = id == null ? null : platformAdminMapper.getById(id);
        if (admin == null) {
            throw new AccountNotFoundException(MessageConstant.PLATFORM_ADMIN_NOT_FOUND);
        }
        admin.setPassword(null);
        return admin;
    }

    @Override
    @Transactional
    public void updateProfile(Long id, PlatformAdminProfileDTO dto) {
        if (platformAdminMapper.getById(id) == null) {
            throw new AccountNotFoundException(MessageConstant.PLATFORM_ADMIN_NOT_FOUND);
        }
        platformAdminMapper.update(PlatformAdmin.builder()
                .id(id)
                .name(dto.getName().trim())
                .build());
        log.info("平台管理员修改资料：id={}, name={}", id, dto.getName());
    }

    // ============================================================
    // 商家入驻审核
    // ============================================================

    @Override
    public PageResult pageMerchants(Integer auditStatus, String keyword, Integer page, Integer pageSize) {
        int p = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();

        PageHelper.startPage(p, size);
        Page<Merchant> result = (Page<Merchant>) merchantMapper.pageByAuditStatus(auditStatus, kw);
        List<Merchant> list = new ArrayList<>();
        for (Merchant m : result.getResult()) {
            m.setPassword(null);
            list.add(m);
        }
        return new PageResult(result.getTotal(), list);
    }

    @Override
    @Transactional
    public void auditMerchant(Long id, MerchantAuditDTO dto) {
        Merchant merchant = merchantMapper.getById(id);
        if (merchant == null) {
            throw new MerchantBusinessException(MessageConstant.MERCHANT_NOT_FOUND);
        }
        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        String reason = dto.getReason() == null ? null : dto.getReason().trim();
        if (!approved && (reason == null || reason.isEmpty())) {
            throw new MerchantBusinessException(MessageConstant.AUDIT_REASON_REQUIRED);
        }

        int newAuditStatus = approved
                ? MerchantAuditStatus.APPROVED.getCode() : MerchantAuditStatus.REJECTED.getCode();
        // 通过 → 可直接营业；驳回 → 保持未营业
        int newStatus = approved ? StatusConstant.ENABLE : StatusConstant.DISABLE;

        int rows = merchantMapper.audit(id, newAuditStatus, newStatus,
                approved ? null : reason, LocalDateTime.now());
        if (rows == 0) {
            throw new MerchantBusinessException(MessageConstant.MERCHANT_ALREADY_AUDITED);
        }
        log.info("商家入驻审核：id={}, shopName={}, 结果={}, 原因={}",
                id, merchant.getShopName(), approved ? "通过" : "驳回", reason);
    }

    // ============================================================
    // 骑手管理
    // ============================================================

    @Override
    public PageResult pageDeliverymen(String keyword, Integer status, Integer page, Integer pageSize) {
        int p = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();

        PageHelper.startPage(p, size);
        Page<Deliveryman> result = (Page<Deliveryman>) deliverymanMapper.pageByCondition(kw, status);
        return new PageResult(result.getTotal(), result.getResult());
    }

    @Override
    @Transactional
    public void updateDeliverymanStatus(Long id, Integer status) {
        if (status == null || (status != RIDER_ENABLED && status != RIDER_DISABLED)) {
            throw new MerchantBusinessException(MessageConstant.DELIVERYMAN_STATUS_INVALID);
        }
        if (deliverymanMapper.getById(id) == null) {
            throw new MerchantBusinessException(MessageConstant.DELIVERYMAN_NOT_FOUND);
        }
        deliverymanMapper.updateStatus(id, status);
        log.info("平台调整骑手状态：id={}, status={}", id, status);
    }

    // ============================================================
    // 看板
    // ============================================================

    @Override
    public PlatformOverviewVO overview() {
        int pending = merchantMapper.countByAuditStatus(MerchantAuditStatus.PENDING.getCode());
        int approved = merchantMapper.countByAuditStatus(MerchantAuditStatus.APPROVED.getCode());
        int rejected = merchantMapper.countByAuditStatus(MerchantAuditStatus.REJECTED.getCode());
        int activeRiders = deliverymanMapper.countByStatus(RIDER_ENABLED);
        int disabledRiders = deliverymanMapper.countByStatus(RIDER_DISABLED);
        return PlatformOverviewVO.builder()
                .pendingMerchants(pending)
                .approvedMerchants(approved)
                .rejectedMerchants(rejected)
                .totalMerchants(pending + approved + rejected)
                .activeRiders(activeRiders)
                .disabledRiders(disabledRiders)
                .totalRiders(activeRiders + disabledRiders)
                .build();
    }
}
