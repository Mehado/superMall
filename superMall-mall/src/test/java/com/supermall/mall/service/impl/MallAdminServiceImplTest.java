package com.supermall.mall.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.constant.StatusConstant;
import com.supermall.exception.AccountNotFoundException;
import com.supermall.exception.LoginFailedException;
import com.supermall.exception.PasswordErrorException;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallAdminLoginDTO;
import com.supermall.mall.entity.MallAdmin;
import com.supermall.mall.mapper.MallAdminMapper;
import com.supermall.mall.properties.MallJwtProperties;
import com.supermall.mall.vo.MallAdminLoginVO;
import com.supermall.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 运营后台管理员登录。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallAdminServiceImpl - 管理员登录")
class MallAdminServiceImplTest {

    private static final String ADMIN_KEY = "mall-admin-secret-key-0123456789abcdef";
    private static final String HASH = "$2a$10$cUuwmfegKK7qRA5bpFNn2eeBi70aSR1XgjEJjQAkPljIxzv3xu7qa"; // Admin1234

    @Mock
    private MallAdminMapper mallAdminMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MallJwtProperties jwtProperties;
    private MallAdminServiceImpl service;

    private MallAdminLoginDTO dto;

    @BeforeEach
    void setUp() {
        jwtProperties = new MallJwtProperties();
        jwtProperties.setAdminSecretKey(ADMIN_KEY);
        jwtProperties.setAdminTtl(7_200_000L);
        jwtProperties.setAdminTokenName("mall-admin-token");

        service = new MallAdminServiceImpl(mallAdminMapper, passwordEncoder, jwtProperties);

        dto = new MallAdminLoginDTO();
        dto.setUsername("admin@qq.com");
        dto.setPassword("Admin1234");
    }

    private MallAdmin admin(int status) {
        return MallAdmin.builder()
                .id(1L).username("admin@qq.com").password(HASH).name("平台管理员").status(status)
                .build();
    }

    @Test
    @DisplayName("登录成功：返回带 token 的信息，且 token 能被管理员密钥解析")
    void loginSuccess() {
        when(mallAdminMapper.getByUsername("admin@qq.com")).thenReturn(admin(StatusConstant.ENABLE));
        when(passwordEncoder.matches("Admin1234", HASH)).thenReturn(true);

        MallAdminLoginVO vo = service.login(dto);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getUsername()).isEqualTo("admin@qq.com");
        assertThat(vo.getName()).isEqualTo("平台管理员");
        assertThat(vo.getToken()).isNotBlank();

        // token 用管理员密钥签发，角色是 MALL_ADMIN
        var claims = JwtUtil.parseJWT(ADMIN_KEY, vo.getToken());
        assertThat(claims.get("role")).isEqualTo(MallMessageConstant.ADMIN);
        assertThat(claims.get("userId").toString()).isEqualTo("1");
    }

    @Test
    @DisplayName("账号不存在")
    void accountNotFound() {
        when(mallAdminMapper.getByUsername(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage(MallMessageConstant.ADMIN_NOT_FOUND);
    }

    @Test
    @DisplayName("密码错误")
    void wrongPassword() {
        when(mallAdminMapper.getByUsername(anyString())).thenReturn(admin(StatusConstant.ENABLE));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(PasswordErrorException.class)
                .hasMessage(MessageConstant.PASSWORD_ERROR);
    }

    @Test
    @DisplayName("账号被禁用")
    void disabled() {
        when(mallAdminMapper.getByUsername(anyString())).thenReturn(admin(StatusConstant.DISABLE));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(LoginFailedException.class)
                .hasMessage(MallMessageConstant.ADMIN_DISABLED);
    }

    @Test
    @DisplayName("查资料时 password 必须置空")
    void getByIdHidesPassword() {
        when(mallAdminMapper.getById(eq(1L))).thenReturn(admin(StatusConstant.ENABLE));

        MallAdmin got = service.getById(1L);

        assertThat(got.getUsername()).isEqualTo("admin@qq.com");
        assertThat(got.getPassword()).isNull();
    }
}
