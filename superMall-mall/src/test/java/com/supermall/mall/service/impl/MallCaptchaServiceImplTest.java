package com.supermall.mall.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.exception.CaptchaException;
import com.supermall.mall.constant.MallRedisKeyConstant;
import com.supermall.properties.CaptchaProperties;
import com.supermall.vo.CaptchaVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 电商验证码：生成写 Redis（mall: 前缀）、一次性校验、开关生效。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallCaptchaServiceImpl - 生成 / 一次性校验 / 前缀隔离")
class MallCaptchaServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private DefaultRedisScript<String> mallConsumeCaptchaScript;

    private CaptchaProperties properties;
    private MallCaptchaServiceImpl captchaService;

    @BeforeEach
    void setUp() {
        properties = new CaptchaProperties();
        // 与 application.yml 保持一致
        properties.setKeyPrefix(MallRedisKeyConstant.CAPTCHA_PREFIX);
        captchaService = new MallCaptchaServiceImpl(stringRedisTemplate, properties, mallConsumeCaptchaScript);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("生成：答案写进 Redis 且带 TTL，返回合法 PNG base64")
    void generateStoresAnswerWithTtl() {
        CaptchaVO vo = captchaService.generate();

        assertThat(vo.getKey()).isNotBlank();
        assertThat(vo.getTtlSeconds()).isEqualTo(properties.getTtlSeconds());

        byte[] png = Base64.getDecoder().decode(vo.getImage());
        assertThat(png[0] & 0xFF).isEqualTo(0x89);
        assertThat(png[1]).isEqualTo((byte) 'P');

        verify(valueOperations).set(startsWith("mall:captcha:"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Redis key 必须走 mall:captcha: 前缀（与外卖的 captcha: 隔离）")
    void redisKeyUsesMallPrefix() {
        doReturn("ABCD").when(stringRedisTemplate)
                .execute(ArgumentMatchers.<RedisScript<String>>any(), anyList());

        captchaService.verifyOrThrow("k1", "ABCD");

        verify(stringRedisTemplate).execute(any(RedisScript.class),
                ArgumentMatchers.eq(List.of("mall:captcha:k1")));
    }

    @Test
    @DisplayName("校验：取值+删除交给 Lua 一次原子完成（不用 GETDEL，Redis 5.0 没有）")
    void verifyUsesSingleAtomicLuaCall() {
        doReturn("ABCD").when(stringRedisTemplate)
                .execute(ArgumentMatchers.<RedisScript<String>>any(), anyList());

        assertThatCode(() -> captchaService.verifyOrThrow("k1", "abcd")).doesNotThrowAnyException();

        verify(stringRedisTemplate, times(1)).execute(any(RedisScript.class), anyList());
        verify(valueOperations, never()).get(anyString());
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("大小写不敏感、首尾空格忽略")
    void verifyCaseInsensitive() {
        doReturn("ABCD").when(stringRedisTemplate)
                .execute(ArgumentMatchers.<RedisScript<String>>any(), anyList());

        assertThatCode(() -> captchaService.verifyOrThrow("k1", "abcd")).doesNotThrowAnyException();
        assertThatCode(() -> captchaService.verifyOrThrow("k2", "  AbCd  ")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("答案不存在（过期/已用过）→ 提示刷新")
    void expiredRejected() {
        doReturn(null).when(stringRedisTemplate)
                .execute(ArgumentMatchers.<RedisScript<String>>any(), anyList());

        assertThatThrownBy(() -> captchaService.verifyOrThrow("k1", "ABCD"))
                .isInstanceOf(CaptchaException.class)
                .hasMessage(MessageConstant.CAPTCHA_EXPIRED);
    }

    @Test
    @DisplayName("答案不匹配 → 验证码不正确")
    void wrongCodeRejected() {
        doReturn("ABCD").when(stringRedisTemplate)
                .execute(ArgumentMatchers.<RedisScript<String>>any(), anyList());

        assertThatThrownBy(() -> captchaService.verifyOrThrow("k1", "ZZZZ"))
                .isInstanceOf(CaptchaException.class)
                .hasMessage(MessageConstant.CAPTCHA_ERROR);
    }

    @Test
    @DisplayName("key 或 code 缺失 → 直接要求先获取验证码，且不碰 Redis")
    void missingParamsRejectedWithoutRedis() {
        assertThatThrownBy(() -> captchaService.verifyOrThrow(null, "ABCD"))
                .isInstanceOf(CaptchaException.class)
                .hasMessage(MessageConstant.CAPTCHA_REQUIRED);
        assertThatThrownBy(() -> captchaService.verifyOrThrow("k1", "  "))
                .isInstanceOf(CaptchaException.class)
                .hasMessage(MessageConstant.CAPTCHA_REQUIRED);

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("开关关闭时直接放行")
    void disabledSkipsVerification() {
        properties.setEnabled(false);

        assertThat(captchaService.isEnabled()).isFalse();
        assertThatCode(() -> captchaService.verifyOrThrow(null, null)).doesNotThrowAnyException();
        verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList());
    }
}
