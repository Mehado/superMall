package com.supermall.takeout.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.exception.LoginFailedException;
import com.supermall.properties.JwtProperties;
import com.supermall.takeout.dto.MerchantApplyDTO;
import com.supermall.takeout.dto.MerchantLoginDTO;
import com.supermall.takeout.entity.Merchant;
import com.supermall.takeout.enumeration.MerchantAuditStatus;
import com.supermall.takeout.mapper.MerchantMapper;
import com.supermall.takeout.vo.MerchantApplyVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商家入驻申请 + 审核门禁：申请落库为待审核；待审 / 驳回的账号不能登录。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MerchantServiceImpl - 入驻申请与审核门禁")
class MerchantOnboardingTest {

    private static final String EMAIL = "newshop@163.com";

    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private MerchantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MerchantServiceImpl(merchantMapper, passwordEncoder, jwtProperties, stringRedisTemplate);
    }

    private MerchantApplyDTO applyDto() {
        MerchantApplyDTO dto = new MerchantApplyDTO();
        dto.setUsername(EMAIL);
        dto.setPassword("Mall1234");
        dto.setShopName("川味小馆");
        dto.setContactName("张三");
        dto.setPhone("13900000099");
        dto.setAddress("北京市朝阳区望京街 9 号");
        return dto;
    }

    @Test
    @DisplayName("首次申请：落库为待审核，回显 auditStatus=0")
    void apply_firstTime_insertsPending() {
        when(merchantMapper.getByUsername(EMAIL)).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("$hash");

        MerchantApplyVO vo = service.apply(applyDto());

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).insertApply(captor.capture());
        assertThat(captor.getValue().getShopName()).isEqualTo("川味小馆");
        assertThat(captor.getValue().getAuditStatus()).isEqualTo(MerchantAuditStatus.PENDING.getCode());
        assertThat(vo.getAuditStatus()).isEqualTo(MerchantAuditStatus.PENDING.getCode());
    }

    @Test
    @DisplayName("邮箱已在审核中 / 已通过：拒绝重复申请")
    void apply_existingNotRejected_isRejected() {
        when(merchantMapper.getByUsername(EMAIL))
                .thenReturn(Merchant.builder().id(1L).username(EMAIL)
                        .auditStatus(MerchantAuditStatus.PENDING.getCode()).build());

        assertThatThrownBy(() -> service.apply(applyDto()))
                .isInstanceOf(LoginFailedException.class)
                .hasMessageContaining(MessageConstant.MERCHANT_APPLY_EXISTS);

        verify(merchantMapper, never()).insertApply(any());
        verify(merchantMapper, never()).reapply(any());
    }

    @Test
    @DisplayName("被驳回后：复用同一行重新提交为待审核")
    void apply_rejected_reapplies() {
        when(merchantMapper.getByUsername(EMAIL))
                .thenReturn(Merchant.builder().id(7L).username(EMAIL)
                        .auditStatus(MerchantAuditStatus.REJECTED.getCode())
                        .rejectReason("执照不清晰").build());
        when(passwordEncoder.encode(anyString())).thenReturn("$hash");
        when(merchantMapper.reapply(any())).thenReturn(1);

        MerchantApplyVO vo = service.apply(applyDto());

        verify(merchantMapper).reapply(any());
        verify(merchantMapper, never()).insertApply(any());
        assertThat(vo.getId()).isEqualTo(7L);
        assertThat(vo.getAuditStatus()).isEqualTo(MerchantAuditStatus.PENDING.getCode());
        assertThat(vo.getRejectReason()).isNull();
    }

    @Test
    @DisplayName("待审核账号登录：被拦且提示审核中")
    void login_pending_isBlocked() {
        when(merchantMapper.getByUsername(EMAIL))
                .thenReturn(Merchant.builder().id(1L).username(EMAIL).password("$hash")
                        .auditStatus(MerchantAuditStatus.PENDING.getCode()).status(0).build());
        when(passwordEncoder.matches("Mall1234", "$hash")).thenReturn(true);

        MerchantLoginDTO dto = new MerchantLoginDTO();
        dto.setUsername(EMAIL);
        dto.setPassword("Mall1234");

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(LoginFailedException.class)
                .hasMessageContaining(MessageConstant.MERCHANT_AUDIT_PENDING);
    }

    @Test
    @DisplayName("已驳回账号登录：被拦并带上驳回原因")
    void login_rejected_isBlockedWithReason() {
        when(merchantMapper.getByUsername(EMAIL))
                .thenReturn(Merchant.builder().id(1L).username(EMAIL).password("$hash")
                        .auditStatus(MerchantAuditStatus.REJECTED.getCode()).status(0)
                        .rejectReason("执照不清晰").build());
        when(passwordEncoder.matches("Mall1234", "$hash")).thenReturn(true);

        MerchantLoginDTO dto = new MerchantLoginDTO();
        dto.setUsername(EMAIL);
        dto.setPassword("Mall1234");

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(LoginFailedException.class)
                .hasMessageContaining(MessageConstant.MERCHANT_AUDIT_REJECTED)
                .hasMessageContaining("执照不清晰");
    }
}
