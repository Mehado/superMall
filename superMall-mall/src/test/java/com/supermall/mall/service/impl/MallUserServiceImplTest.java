package com.supermall.mall.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.constant.StatusConstant;
import com.supermall.dto.UserLoginDTO;
import com.supermall.dto.UserRegisterDTO;
import com.supermall.entity.User;
import com.supermall.exception.AccountNotFoundException;
import com.supermall.exception.LoginFailedException;
import com.supermall.exception.PasswordErrorException;
import com.supermall.mall.mapper.MallUserMapper;
import com.supermall.properties.JwtProperties;
import com.supermall.utils.JwtUtil;
import com.supermall.vo.UserLoginVO;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商城买家账号（读写共享表 sys_user）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallUserServiceImpl - 买家登录 / 注册")
class MallUserServiceImplTest {

    private static final String USER_KEY = "supermall-user-secret-key-0123456789abcdef";
    private static final String HASH = "$2a$10$QFQJ0maLjRisP1Fs5A5dE.PIDcnyLGm3xnuIniRrvbZpiGWGwgg/6"; // Mall1234

    @Mock
    private MallUserMapper mallUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MallUserServiceImpl service;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setUserSecretKey(USER_KEY);
        jwtProperties.setUserTtl(7_200_000L);
        jwtProperties.setUserTokenName("token");

        service = new MallUserServiceImpl(mallUserMapper, passwordEncoder, jwtProperties);
    }

    private User user(int status) {
        return User.builder()
                .id(1L).username("user1@qq.com").password(HASH).nickname("小明").status(status)
                .build();
    }

    @Test
    @DisplayName("登录成功：token 用【外卖同一把 user 密钥】签发（这是账号互通的关键）")
    void loginIssuesTokenWithSharedUserKey() {
        when(mallUserMapper.getByUsername("user1@qq.com")).thenReturn(user(StatusConstant.ENABLE));
        when(passwordEncoder.matches("Mall1234", HASH)).thenReturn(true);

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("user1@qq.com");
        dto.setPassword("Mall1234");

        UserLoginVO vo = service.login(dto);

        assertThat(vo.getNickname()).isEqualTo("小明");
        // 关键断言：用外卖的 user 密钥就能解析 → 证明两边 token 通用
        var claims = JwtUtil.parseJWT(USER_KEY, vo.getToken());
        assertThat(claims.get("role")).isEqualTo("USER");
        assertThat(claims.get("userId").toString()).isEqualTo("1");
    }

    @Test
    @DisplayName("账号不存在 / 密码错 / 被禁用")
    void loginFailures() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("user1@qq.com");
        dto.setPassword("Mall1234");

        when(mallUserMapper.getByUsername(anyString())).thenReturn(null);
        assertThatThrownBy(() -> service.login(dto)).isInstanceOf(AccountNotFoundException.class);

        when(mallUserMapper.getByUsername(anyString())).thenReturn(user(StatusConstant.ENABLE));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(PasswordErrorException.class)
                .hasMessage(MessageConstant.PASSWORD_ERROR);

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(mallUserMapper.getByUsername(anyString())).thenReturn(user(StatusConstant.DISABLE));
        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(LoginFailedException.class)
                .hasMessage(MessageConstant.ACCOUNT_LOCKED);
    }

    @Test
    @DisplayName("注册成功：写入共享 sys_user，昵称默认取邮箱 @ 前部分，并直接返回 token")
    void registerWritesSharedTableAndReturnsToken() {
        when(mallUserMapper.getByUsername(anyString())).thenReturn(null);
        when(passwordEncoder.encode("Mall1234")).thenReturn(HASH);

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("newbie@qq.com");
        dto.setPassword("Mall1234");
        // 不传 nickname

        UserLoginVO vo = service.register(dto);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(mallUserMapper).insert(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("newbie@qq.com");
        assertThat(saved.getValue().getNickname()).isEqualTo("newbie");
        assertThat(saved.getValue().getPassword()).isEqualTo(HASH);
        assertThat(saved.getValue().getStatus()).isEqualTo(StatusConstant.ENABLE);
        assertThat(saved.getValue().getCreateTime()).isNotNull();

        assertThat(vo.getToken()).isNotBlank();
        assertThat(JwtUtil.parseJWT(USER_KEY, vo.getToken())).isNotNull();
    }

    @Test
    @DisplayName("注册时传了昵称就用传的")
    void registerKeepsGivenNickname() {
        when(mallUserMapper.getByUsername(anyString())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn(HASH);

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("newbie@qq.com");
        dto.setPassword("Mall1234");
        dto.setNickname("商城新人");

        service.register(dto);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(mallUserMapper).insert(saved.capture());
        assertThat(saved.getValue().getNickname()).isEqualTo("商城新人");
    }

    @Test
    @DisplayName("邮箱已注册 → 拒绝")
    void registerRejectsDuplicate() {
        when(mallUserMapper.getByUsername("user1@qq.com")).thenReturn(user(StatusConstant.ENABLE));

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("user1@qq.com");
        dto.setPassword("Mall1234");

        assertThatThrownBy(() -> service.register(dto))
                .isInstanceOf(LoginFailedException.class)
                .hasMessage(MessageConstant.USERNAME_EXISTS);
    }

    @Test
    @DisplayName("查资料时 password 置空")
    void getByIdHidesPassword() {
        when(mallUserMapper.getById(eq(1L))).thenReturn(user(StatusConstant.ENABLE));

        User got = service.getById(1L);

        assertThat(got.getNickname()).isEqualTo("小明");
        assertThat(got.getPassword()).isNull();
    }
}
