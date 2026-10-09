package com.supermall.takeout.service.impl;

import com.supermall.takeout.constant.RedisKeyConstant;
import com.supermall.takeout.entity.Merchant;
import com.supermall.takeout.exception.MerchantBusinessException;
import com.github.pagehelper.Page;
import com.supermall.takeout.mapper.MerchantMapper;
import com.supermall.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 商家营业状态：缓存读 / DB 回源 / 写穿缓存 的单元测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MerchantServiceImpl - 营业状态")
class MerchantServiceImplTest {

    private static final Long MERCHANT_ID = 1L;
    private static final String KEY = RedisKeyConstant.MERCHANT_BUSINESS_PREFIX + MERCHANT_ID;

    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private MerchantServiceImpl service;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        service = new MerchantServiceImpl(merchantMapper, passwordEncoder, jwtProperties, stringRedisTemplate);
    }

    @Test
    @DisplayName("缓存命中：直接返回，不查 DB")
    void getBusinessStatus_cacheHit_returnsCachedWithoutDb() {
        when(valueOps.get(KEY)).thenReturn("0");

        assertThat(service.getBusinessStatus(MERCHANT_ID)).isZero();
        verify(merchantMapper, never()).getBusinessStatusById(any());
    }

    @Test
    @DisplayName("缓存未命中：回源 DB 并回写缓存")
    void getBusinessStatus_cacheMiss_readsDbAndWritesBack() {
        when(valueOps.get(KEY)).thenReturn(null);
        when(merchantMapper.getBusinessStatusById(MERCHANT_ID)).thenReturn(0);

        assertThat(service.getBusinessStatus(MERCHANT_ID)).isZero();

        verify(merchantMapper).getBusinessStatusById(MERCHANT_ID);
        verify(valueOps).set(eq(KEY), eq("0"), any(Duration.class));
    }

    @Test
    @DisplayName("缓存未命中且 DB 无值：默认营业中")
    void getBusinessStatus_cacheMissAndDbNull_defaultsToOpen() {
        when(valueOps.get(KEY)).thenReturn(null);
        when(merchantMapper.getBusinessStatusById(MERCHANT_ID)).thenReturn(null);

        assertThat(service.getBusinessStatus(MERCHANT_ID)).isEqualTo(1);

        verify(valueOps).set(eq(KEY), eq("1"), any(Duration.class));
    }

    @Test
    @DisplayName("非法营业状态：拒绝且不落库")
    void updateBusinessStatus_invalidCode_isRejected() {
        assertThatThrownBy(() -> service.updateBusinessStatus(MERCHANT_ID, 9))
                .isInstanceOf(MerchantBusinessException.class);

        verifyNoInteractions(merchantMapper);
    }

    @Test
    @DisplayName("正常切换：先写 DB 再写穿缓存")
    void updateBusinessStatus_writesDbThenCache() {
        when(merchantMapper.updateBusinessStatus(MERCHANT_ID, 0)).thenReturn(1);

        service.updateBusinessStatus(MERCHANT_ID, 0);

        verify(merchantMapper).updateBusinessStatus(MERCHANT_ID, 0);
        verify(valueOps).set(eq(KEY), eq("0"), any(Duration.class));
    }

    @Test
    @DisplayName("商家不存在：抛业务异常")
    void updateBusinessStatus_merchantMissing_throws() {
        when(merchantMapper.updateBusinessStatus(eq(MERCHANT_ID), any())).thenReturn(0);

        assertThatThrownBy(() -> service.updateBusinessStatus(MERCHANT_ID, 1))
                .isInstanceOf(MerchantBusinessException.class);
    }

    @Test
    @DisplayName("商家列表直接带出 DB 的营业状态")
    void listOpen_carriesBusinessStatusFromDb() {
        // 分页服务用 (Page) 强转 PageHelper 的返回值，所以这里 stub 一个真 Page（同 MallFavoriteServiceImplTest 的写法）
        Page<Merchant> page = new Page<>(1, 20);
        page.setTotal(2);
        page.add(Merchant.builder().id(1L).shopName("老王牛肉面").businessStatus(0).build());
        page.add(Merchant.builder().id(2L).shopName("轻食沙拉铺").businessStatus(1).build());
        when(merchantMapper.listOpen(isNull())).thenReturn(page);

        // 列表已分页（2026-09-21）：返回 PageResult，记录仍按顺序带出 businessStatus
        List<?> open = service.listOpen(null, 1, 20).getRecords();

        assertThat(open).hasSize(2);
        assertThat(((Merchant) open.get(0)).getBusinessStatus()).isZero();
        assertThat(((Merchant) open.get(1)).getBusinessStatus()).isEqualTo(1);
    }
}
