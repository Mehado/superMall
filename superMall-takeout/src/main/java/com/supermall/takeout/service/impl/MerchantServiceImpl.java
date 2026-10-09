package com.supermall.takeout.service.impl;

import com.supermall.constant.JwtClaimsConstant;
import com.supermall.constant.MessageConstant;
import com.supermall.takeout.constant.RedisKeyConstant;
import com.supermall.constant.RoleConstant;
import com.supermall.constant.StatusConstant;
import com.supermall.takeout.annotations.RequireCaptcha;
import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.MerchantApplyDTO;
import com.supermall.takeout.dto.MerchantLoginDTO;
import com.supermall.takeout.dto.MerchantUpdateDTO;
import com.supermall.takeout.entity.Merchant;
import com.supermall.takeout.enumeration.MerchantAuditStatus;
import com.supermall.exception.AccountNotFoundException;
import com.supermall.exception.LoginFailedException;
import com.supermall.takeout.exception.MerchantBusinessException;
import com.supermall.exception.PasswordErrorException;
import com.supermall.takeout.mapper.MerchantMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.properties.JwtProperties;
import com.supermall.result.PageResult;
import com.supermall.takeout.service.MerchantService;
import com.supermall.utils.JwtUtil;
import com.supermall.utils.RichTextSanitizer;
import com.supermall.takeout.vo.MerchantApplyVO;
import com.supermall.takeout.vo.MerchantLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;

    /** 营业状态缓存有效期：到期回源 DB，防止缓存与库长期不一致 */
    private static final Duration BUSINESS_CACHE_TTL = Duration.ofMinutes(30);

    @Override
    @RequireCaptcha
    public MerchantLoginVO login(MerchantLoginDTO dto) {
        Merchant merchant = merchantMapper.getByUsername(dto.getUsername());
        if (merchant == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getPassword(), merchant.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        // 入驻审核优先于营业状态判断：待审 / 驳回的商家给出明确原因，
        // 否则 status=0 会被误报成「商家已停业」。
        int auditStatus = auditStatusOf(merchant);
        if (auditStatus == MerchantAuditStatus.PENDING.getCode()) {
            throw new LoginFailedException(MessageConstant.MERCHANT_AUDIT_PENDING);
        }
        if (auditStatus == MerchantAuditStatus.REJECTED.getCode()) {
            String reason = merchant.getRejectReason() == null ? "" : "：" + merchant.getRejectReason();
            throw new LoginFailedException(MessageConstant.MERCHANT_AUDIT_REJECTED + reason);
        }
        if (StatusConstant.DISABLE.equals(merchant.getStatus())) {
            throw new LoginFailedException(MessageConstant.MERCHANT_DISABLED);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.MERCHANT_ID, merchant.getId());
        claims.put(JwtClaimsConstant.USERNAME, merchant.getUsername());
        claims.put(JwtClaimsConstant.ROLE, RoleConstant.MERCHANT);
        String token = JwtUtil.createJWT(jwtProperties.getMerchantSecretKey(), jwtProperties.getMerchantTtl(), claims);

        return MerchantLoginVO.builder()
                .id(merchant.getId())
                .username(merchant.getUsername())
                .shopName(merchant.getShopName())
                .token(token)
                .build();
    }

    @Override
    @Transactional
    @RequireCaptcha
    public MerchantApplyVO apply(MerchantApplyDTO dto) {
        Merchant existing = merchantMapper.getByUsername(dto.getUsername());
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            Merchant merchant = Merchant.builder()
                    .username(dto.getUsername())
                    .password(passwordEncoder.encode(dto.getPassword()))
                    .shopName(dto.getShopName().trim())
                    .contactName(dto.getContactName().trim())
                    .phone(dto.getPhone())
                    .address(dto.getAddress().trim())
                    // 店铺简介是纯文本：入库前剥掉标签（防存储型 XSS）
                    .description(RichTextSanitizer.plainText(dto.getDescription()))
                    .licenseImage(dto.getLicenseImage())
                    // 与 insertApply 落库一致（SQL 里固定写 0），否则回显会被当成已通过
                    .auditStatus(MerchantAuditStatus.PENDING.getCode())
                    .applyTime(now)
                    .createTime(now)
                    .updateTime(now)
                    .build();
            // insertApply 内部固定写 status=0、audit_status=0
            merchantMapper.insertApply(merchant);
            log.info("收到商家入驻申请：id={}, username={}, shopName={}",
                    merchant.getId(), merchant.getUsername(), merchant.getShopName());
            return toApplyVO(merchant, "入驻申请已提交，请等待平台审核");
        }

        // 已存在：只有「被驳回」的可以重提；审核中 / 已通过都不再受理
        if (auditStatusOf(existing) != MerchantAuditStatus.REJECTED.getCode()) {
            throw new LoginFailedException(MessageConstant.MERCHANT_APPLY_EXISTS);
        }
        existing.setPassword(passwordEncoder.encode(dto.getPassword()));
        existing.setShopName(dto.getShopName().trim());
        existing.setContactName(dto.getContactName().trim());
        existing.setPhone(dto.getPhone());
        existing.setAddress(dto.getAddress().trim());
        existing.setDescription(RichTextSanitizer.plainText(dto.getDescription()));
        existing.setLicenseImage(dto.getLicenseImage());
        existing.setApplyTime(now);
        // reapply 带 where audit_status=2，并发重提只会成功一次
        if (merchantMapper.reapply(existing) == 0) {
            throw new LoginFailedException(MessageConstant.MERCHANT_APPLY_EXISTS);
        }
        existing.setAuditStatus(MerchantAuditStatus.PENDING.getCode());
        existing.setRejectReason(null);
        log.info("商家重新提交入驻申请：id={}, username={}", existing.getId(), existing.getUsername());
        return toApplyVO(existing, "入驻申请已重新提交，请等待平台审核");
    }

    @Override
    public MerchantApplyVO applyStatus(String username) {
        Merchant merchant = username == null ? null : merchantMapper.getByUsername(username);
        if (merchant == null) {
            throw new MerchantBusinessException(MessageConstant.MERCHANT_NOT_FOUND);
        }
        String message = switch (auditStatusOf(merchant)) {
            case 0 -> "入驻申请审核中，请耐心等待";
            case 2 -> "入驻申请未通过" + (merchant.getRejectReason() == null ? "" : "：" + merchant.getRejectReason());
            default -> "审核已通过，请直接登录商家端";
        };
        return toApplyVO(merchant, message);
    }

    /** auditStatus 为 null 时视为已通过（兼容历史数据） */
    private int auditStatusOf(Merchant merchant) {
        return merchant.getAuditStatus() == null
                ? MerchantAuditStatus.APPROVED.getCode() : merchant.getAuditStatus();
    }

    private MerchantApplyVO toApplyVO(Merchant merchant, String message) {
        return MerchantApplyVO.builder()
                .id(merchant.getId())
                .username(merchant.getUsername())
                .shopName(merchant.getShopName())
                .auditStatus(auditStatusOf(merchant))
                .auditStatusDesc(MerchantAuditStatus.descOf(auditStatusOf(merchant)))
                .rejectReason(merchant.getRejectReason())
                .message(message)
                .build();
    }

    @Override
    public Merchant getById(Long id) {
        // businessStatus 由 select * 直接从 DB 带出
        return merchantMapper.getById(id);
    }

    @Override
    public void updateShop(MerchantUpdateDTO dto) {
        Long merchantId = BaseContext.getCurrentId();
        merchantMapper.update(Merchant.builder()
                .id(merchantId)
                .shopName(dto.getShopName())
                .logo(dto.getLogo())
                .description(RichTextSanitizer.plainText(dto.getDescription()))
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .status(dto.getStatus())
                .build());
    }

    @Override
    public PageResult listOpen(String keyword, Integer page, Integer pageSize) {
        int p = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        PageHelper.startPage(p, size);
        // listOpen 的 SQL 已带出 business_status
        Page<Merchant> result = (Page<Merchant>) merchantMapper.listOpen(keyword);
        return new PageResult(result.getTotal(), result.getResult());
    }

    @Override
    public Merchant getDetail(Long id) {
        Merchant merchant = merchantMapper.getById(id);
        if (merchant == null) {
            throw new MerchantBusinessException(MessageConstant.MERCHANT_NOT_FOUND);
        }
        merchant.setPassword(null);
        // businessStatus 由 select * 直接从 DB 带出
        return merchant;
    }

    @Override
    public Integer getBusinessStatus(Long merchantId) {
        String key = RedisKeyConstant.MERCHANT_BUSINESS_PREFIX + merchantId;
        // 1. 先读缓存
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            return Integer.valueOf(cached);
        }
        // 2. 缓存未命中：回源 DB（DB 为准），再回写缓存
        Integer fromDb = merchantMapper.getBusinessStatusById(merchantId);
        Integer status = fromDb == null ? StatusConstant.ENABLE : fromDb;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(status), BUSINESS_CACHE_TTL);
        return status;
    }

    @Override
    @Transactional
    public void updateBusinessStatus(Long merchantId, Integer businessStatus) {
        if (businessStatus == null
                || (!StatusConstant.ENABLE.equals(businessStatus) && !StatusConstant.DISABLE.equals(businessStatus))) {
            throw new MerchantBusinessException(MessageConstant.BUSINESS_STATUS_INVALID);
        }
        // 1. 先写 DB（DB 为准）
        int rows = merchantMapper.updateBusinessStatus(merchantId, businessStatus);
        if (rows == 0) {
            throw new MerchantBusinessException(MessageConstant.MERCHANT_NOT_FOUND);
        }
        // 2. 写穿缓存：同步刷新 Redis
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstant.MERCHANT_BUSINESS_PREFIX + merchantId,
                String.valueOf(businessStatus),
                BUSINESS_CACHE_TTL);
        log.info("商家营业状态更新：merchantId={}, businessStatus={}", merchantId, businessStatus);
    }
}
