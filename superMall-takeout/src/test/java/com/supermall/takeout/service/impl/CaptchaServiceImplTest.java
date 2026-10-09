package com.supermall.takeout.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.exception.CaptchaException;
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
 * 图形验证码：生成写 Redis、校验一次性、开关生效。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CaptchaServiceImpl - 生成 / 一次性校验 / 开关")
class CaptchaServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private DefaultRedisScript<String> consumeScript;

    private CaptchaProperties properties;
    private CaptchaServiceImpl captchaService;

    @BeforeEach
    void setUp() {
        properties = new CaptchaProperties();
        captchaService = new CaptchaServiceImpl(stringRedisTemplate, properties, consumeScript);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("生成：答案写进 Redis 并带 TTL，返回 base64 PNG")
    void generateStoresAnswerWithTtl() {
        CaptchaVO vo = captchaService.generate();

        assertThat(vo.getKey()).isNotBlank();
        assertThat(vo.getTtlSeconds()).isEqualTo(properties.getTtlSeconds());

        // 返回的是可直接放进 <img src="data:image/png;base64,..."> 的内容
        assertThat(vo.getImage()).isNotBlank();
        byte[] png = Base64.getDecoder().decode(vo.getImage());
        assertThat(png[0] & 0xFF).isEqualTo(0x89); // PNG 魔数
        assertThat(png[1]).isEqualTo((byte) 'P');

        verify(valueOperations).set(startsWith("captcha:"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("校验通过后答案已被删除（一次性，用 Lua 原子取值+删除）")
    void verifyConsumesAnswer() {
        doReturn("ABCD").when(stringRedisTemplate)
                .execute(ArgumentMatchers.<RedisScript<String>>any(), anyList());

        assertThatCode(() -> captchaService.verifyOrThrow("k1", "ABCD"))
                .doesNotThrowAnyException();

        verify(stringRedisTemplate).execute(any(RedisScript.class), anyList());
    }

    @Test
    @DisplayName("大小写不敏感、首尾空格被忽略")
    void verifyIsCaseInsensitive() {
        doReturn("ABCD").when(stringRedisTemplate)
                .execute(ArgumentMatchers.<RedisScript<String>>any(), anyList());

        assertThatCode(() -> captchaService.verifyOrThrow("k1", "abcd")).doesNotThrowAnyException();
        assertThatCode(() -> captchaService.verifyOrThrow("k2", "  AbCd  ")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("答案不存在（过期 / 已用过）→ 提示刷新")
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
    @DisplayName("开关关闭时直接放行（供压测 / 自动化使用）")
    void disabledSkipsVerification() {
        properties.setEnabled(false);

        assertThat(captchaService.isEnabled()).isFalse();
        assertThatCode(() -> captchaService.verifyOrThrow(null, null)).doesNotThrowAnyException();

        verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList());
    }

    @Test
    @DisplayName("默认开启")
    void enabledByDefault() {
        assertThat(captchaService.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("每次生成的 key 都不同，且答案长度符合配置")
    void keysAreUniqueAndLengthHonoured() {
        properties.setLength(6);
        CaptchaVO a = captchaService.generate();
        CaptchaVO b = captchaService.generate();

        assertThat(a.getKey()).isNotEqualTo(b.getKey());
        // 两次写入的答案长度都等于配置的 length
        verify(valueOperations, times(2)).set(
                startsWith("captcha:"),
                org.mockito.ArgumentMatchers.argThat(v -> v != null && v.length() == 6),
                any(Duration.class));
    }

    @Test
    @DisplayName("Lua 脚本只被调用一次（不存在 GET 后再 DEL 的两次往返）")
    void singleRoundTrip() {
        doReturn("ABCD").when(stringRedisTemplate)
                .execute(ArgumentMatchers.<RedisScript<String>>any(), anyList());

        captchaService.verifyOrThrow("k1", "ABCD");

        verify(stringRedisTemplate, times(1)).execute(any(RedisScript.class), anyList());
        // 不应走 opsForValue 的 get/delete（那是非原子做法）
        verify(valueOperations, never()).get(anyString());
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("Redis key 带配置的前缀")
    void redisKeyUsesPrefix() {
        properties.setKeyPrefix("cap:");
        doReturn("ABCD").when(stringRedisTemplate)
                .execute(ArgumentMatchers.<RedisScript<String>>any(), anyList());

        captchaService.verifyOrThrow("k1", "ABCD");

        verify(stringRedisTemplate).execute(any(RedisScript.class),
                org.mockito.ArgumentMatchers.eq(List.of("cap:k1")));
    }
}
