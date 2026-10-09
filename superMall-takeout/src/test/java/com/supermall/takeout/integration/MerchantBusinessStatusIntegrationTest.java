package com.supermall.takeout.integration;

import com.supermall.takeout.constant.RedisKeyConstant;
import com.supermall.takeout.entity.Merchant;
import com.supermall.takeout.exception.MerchantBusinessException;
import com.supermall.takeout.mapper.MerchantMapper;
import com.supermall.takeout.service.MerchantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 集成测试：连接真实的 MySQL / Redis，验证「DB 为准 + Redis 缓存」的完整链路。
 * <p>
 * 依赖本地 MySQL / Redis / RabbitMQ 已启动（与运行应用一致）。
 * DB 变更由测试事务回滚；Redis 变更在 {@link #cleanCache()} 中清理。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@DisplayName("集成：商家营业状态 DB + Redis 缓存")
class MerchantBusinessStatusIntegrationTest {

    private static final Long MERCHANT_ID = 1L;
    private static final String KEY = RedisKeyConstant.MERCHANT_BUSINESS_PREFIX + MERCHANT_ID;

    @Autowired
    private MerchantService merchantService;
    @Autowired
    private MerchantMapper merchantMapper;
    @Autowired
    private StringRedisTemplate redis;

    @AfterEach
    void cleanCache() {
        redis.delete(KEY);
    }

    @Test
    @DisplayName("切换营业状态：先落 DB，再写穿缓存")
    void toggle_persistsToDbAndRefreshesCache() {
        merchantService.updateBusinessStatus(MERCHANT_ID, 0);

        assertThat(merchantMapper.getBusinessStatusById(MERCHANT_ID)).isZero();
        assertThat(redis.opsForValue().get(KEY)).isEqualTo("0");
    }

    @Test
    @DisplayName("缓存失效：回源 DB 并回写缓存（Redis 重启也不丢状态）")
    void cacheMiss_fallsBackToDbAndRewritesCache() {
        merchantService.updateBusinessStatus(MERCHANT_ID, 0);

        // 模拟 Redis 丢失
        redis.delete(KEY);
        assertThat(redis.opsForValue().get(KEY)).isNull();

        assertThat(merchantService.getBusinessStatus(MERCHANT_ID)).isZero();
        assertThat(redis.opsForValue().get(KEY)).isEqualTo("0");
    }

    @Test
    @DisplayName("用户端商家列表带出营业状态")
    void listOpen_carriesBusinessStatus() {
        merchantService.updateBusinessStatus(MERCHANT_ID, 0);

        // 列表已分页（2026-09-21）：page 从 1 开始
        List<Merchant> open = merchantService.listOpen(null, 1, 50).getRecords().stream()
                .map(o -> (Merchant) o)
                .toList();
        Merchant target = open.stream()
                .filter(m -> MERCHANT_ID.equals(m.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(target.getBusinessStatus()).isZero();
    }

    @Test
    @DisplayName("非法营业状态：拒绝")
    void invalidBusinessStatus_isRejected() {
        assertThatThrownBy(() -> merchantService.updateBusinessStatus(MERCHANT_ID, 9))
                .isInstanceOf(MerchantBusinessException.class);
    }
}
