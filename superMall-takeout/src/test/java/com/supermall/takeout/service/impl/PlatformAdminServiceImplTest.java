package com.supermall.takeout.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.constant.StatusConstant;
import com.supermall.exception.AccountNotFoundException;
import com.supermall.exception.PasswordErrorException;
import com.supermall.properties.JwtProperties;
import com.supermall.takeout.dto.MerchantAuditDTO;
import com.supermall.takeout.dto.PlatformAdminLoginDTO;
import com.supermall.takeout.dto.PlatformAdminProfileDTO;
import com.supermall.takeout.entity.Merchant;
import com.supermall.takeout.entity.PlatformAdmin;
import com.supermall.takeout.enumeration.MerchantAuditStatus;
import com.supermall.takeout.exception.MerchantBusinessException;
import com.supermall.takeout.mapper.DeliverymanMapper;
import com.supermall.takeout.mapper.MerchantMapper;
import com.supermall.takeout.mapper.PlatformAdminMapper;
import com.supermall.takeout.vo.PlatformOverviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 平台运营：登录鉴权 + 入驻审核的条件更新语义。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PlatformAdminServiceImpl - 平台运营")
class PlatformAdminServiceImplTest {

    private static final Long MERCHANT_ID = 9L;

    @Mock
    private PlatformAdminMapper platformAdminMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private DeliverymanMapper deliverymanMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProperties jwtProperties;

    private PlatformAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PlatformAdminServiceImpl(platformAdminMapper, merchantMapper,
                deliverymanMapper, passwordEncoder, jwtProperties);
    }

    /* ---------------- 登录 ---------------- */

    @Test
    @DisplayName("账号不存在 → AccountNotFoundException")
    void login_accountMissing_throws() {
        when(platformAdminMapper.getByUsername("nobody@163.com")).thenReturn(null);

        PlatformAdminLoginDTO dto = new PlatformAdminLoginDTO();
        dto.setUsername("nobody@163.com");
        dto.setPassword("Admin1234");

        assertThatThrownBy(() -> service.login(dto)).isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("密码错误 → PasswordErrorException")
    void login_wrongPassword_throws() {
        when(platformAdminMapper.getByUsername("platform@163.com"))
                .thenReturn(PlatformAdmin.builder().id(1L).password("$hash").status(1).build());
        when(passwordEncoder.matches("bad", "$hash")).thenReturn(false);

        PlatformAdminLoginDTO dto = new PlatformAdminLoginDTO();
        dto.setUsername("platform@163.com");
        dto.setPassword("bad");

        assertThatThrownBy(() -> service.login(dto)).isInstanceOf(PasswordErrorException.class);
    }

    /* ---------------- 审核 ---------------- */

    @Test
    @DisplayName("驳回不填原因 → 拒绝且不落库")
    void auditMerchant_rejectWithoutReason_isRejected() {
        when(merchantMapper.getById(MERCHANT_ID))
                .thenReturn(Merchant.builder().id(MERCHANT_ID).shopName("川味小馆").build());

        MerchantAuditDTO dto = new MerchantAuditDTO();
        dto.setApproved(false);
        dto.setReason("   ");

        assertThatThrownBy(() -> service.auditMerchant(MERCHANT_ID, dto))
                .isInstanceOf(MerchantBusinessException.class)
                .hasMessageContaining(MessageConstant.AUDIT_REASON_REQUIRED);

        verify(merchantMapper, never()).audit(anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("通过 → audit_status=1 且店铺可直接营业，驳回原因清空")
    void auditMerchant_approve_setsApprovedAndOpen() {
        when(merchantMapper.getById(MERCHANT_ID))
                .thenReturn(Merchant.builder().id(MERCHANT_ID).shopName("川味小馆").build());
        when(merchantMapper.audit(eq(MERCHANT_ID), any(), any(), any(), any())).thenReturn(1);

        MerchantAuditDTO dto = new MerchantAuditDTO();
        dto.setApproved(true);

        service.auditMerchant(MERCHANT_ID, dto);

        ArgumentCaptor<Integer> auditStatus = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> status = ArgumentCaptor.forClass(Integer.class);
        verify(merchantMapper).audit(eq(MERCHANT_ID), auditStatus.capture(), status.capture(),
                isNull(), any());
        assertThat(auditStatus.getValue()).isEqualTo(MerchantAuditStatus.APPROVED.getCode());
        assertThat(status.getValue()).isEqualTo(StatusConstant.ENABLE);
    }

    @Test
    @DisplayName("驳回 → audit_status=2 且店铺保持未营业，原因落库")
    void auditMerchant_reject_setsRejectedAndClosed() {
        when(merchantMapper.getById(MERCHANT_ID))
                .thenReturn(Merchant.builder().id(MERCHANT_ID).shopName("川味小馆").build());
        when(merchantMapper.audit(eq(MERCHANT_ID), any(), any(), any(), any())).thenReturn(1);

        MerchantAuditDTO dto = new MerchantAuditDTO();
        dto.setApproved(false);
        dto.setReason("执照不清晰");

        service.auditMerchant(MERCHANT_ID, dto);

        ArgumentCaptor<Integer> auditStatus = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> status = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(merchantMapper).audit(eq(MERCHANT_ID), auditStatus.capture(), status.capture(),
                reason.capture(), any());
        assertThat(auditStatus.getValue()).isEqualTo(MerchantAuditStatus.REJECTED.getCode());
        assertThat(status.getValue()).isEqualTo(StatusConstant.DISABLE);
        assertThat(reason.getValue()).isEqualTo("执照不清晰");
    }

    @Test
    @DisplayName("重复审核（条件更新 0 行）→ 抛「已审核」")
    void auditMerchant_alreadyAudited_throws() {
        when(merchantMapper.getById(MERCHANT_ID))
                .thenReturn(Merchant.builder().id(MERCHANT_ID).build());
        when(merchantMapper.audit(eq(MERCHANT_ID), any(), any(), any(), any())).thenReturn(0);

        MerchantAuditDTO dto = new MerchantAuditDTO();
        dto.setApproved(true);

        assertThatThrownBy(() -> service.auditMerchant(MERCHANT_ID, dto))
                .isInstanceOf(MerchantBusinessException.class)
                .hasMessageContaining(MessageConstant.MERCHANT_ALREADY_AUDITED);
    }

    /* ---------------- 我的资料 ---------------- */

    @Test
    @DisplayName("查自己的资料：password 置空")
    void getById_blanksPassword() {
        when(platformAdminMapper.getById(1L)).thenReturn(
                PlatformAdmin.builder().id(1L).username("platform@163.com").password("$hash").name("平台运营").build());

        PlatformAdmin admin = service.getById(1L);

        assertThat(admin.getPassword()).isNull();
        assertThat(admin.getName()).isEqualTo("平台运营");
    }

    @Test
    @DisplayName("资料不存在 → AccountNotFoundException")
    void getById_missing_throws() {
        when(platformAdminMapper.getById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("改姓名：去空格后落库")
    void updateProfile_trimsName() {
        when(platformAdminMapper.getById(1L)).thenReturn(PlatformAdmin.builder().id(1L).build());
        when(platformAdminMapper.update(any())).thenReturn(1);

        PlatformAdminProfileDTO dto = new PlatformAdminProfileDTO();
        dto.setName("  新名字  ");
        service.updateProfile(1L, dto);

        ArgumentCaptor<PlatformAdmin> captor = ArgumentCaptor.forClass(PlatformAdmin.class);
        verify(platformAdminMapper).update(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("新名字");
        assertThat(captor.getValue().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("改不存在的管理员 → 报错且不落库")
    void updateProfile_missing_throws() {
        when(platformAdminMapper.getById(99L)).thenReturn(null);

        PlatformAdminProfileDTO dto = new PlatformAdminProfileDTO();
        dto.setName("x");

        assertThatThrownBy(() -> service.updateProfile(99L, dto))
                .isInstanceOf(AccountNotFoundException.class);
        verify(platformAdminMapper, never()).update(any());
    }

    /* ---------------- 看板 ---------------- */

    @Test
    @DisplayName("看板汇总各审核状态与骑手数")
    void overview_aggregatesCounts() {
        when(merchantMapper.countByAuditStatus(MerchantAuditStatus.PENDING.getCode())).thenReturn(2);
        when(merchantMapper.countByAuditStatus(MerchantAuditStatus.APPROVED.getCode())).thenReturn(3);
        when(merchantMapper.countByAuditStatus(MerchantAuditStatus.REJECTED.getCode())).thenReturn(1);
        when(deliverymanMapper.countByStatus(1)).thenReturn(4);
        when(deliverymanMapper.countByStatus(0)).thenReturn(1);

        PlatformOverviewVO vo = service.overview();

        assertThat(vo.getPendingMerchants()).isEqualTo(2);
        assertThat(vo.getTotalMerchants()).isEqualTo(6);
        assertThat(vo.getActiveRiders()).isEqualTo(4);
        assertThat(vo.getTotalRiders()).isEqualTo(5);
    }
}
