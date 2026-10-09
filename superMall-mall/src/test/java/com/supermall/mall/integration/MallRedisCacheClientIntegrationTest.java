package com.supermall.mall.integration;

import com.alibaba.fastjson.TypeReference;
import com.supermall.mall.constant.MallRedisKeyConstant;
import com.supermall.mall.utils.MallRedisCacheClient;
import com.supermall.mall.vo.MallProductVO;
import com.supermall.mall.vo.MallSkuVO;
import com.supermall.mall.vo.MallSpecVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试：连接**真实的 Redis**，验证商品详情缓存工具（三防）的实际行为。
 *
 * <p>单测（{@code MallProductCacheTest}）验证的是「什么情况下调用 setJson / setNull / tryLock」；
 * 这里验证的是「调用真的落到 Redis 上，且行为正确」：
 * <ul>
 *   <li>写 JSON 缓存：TTL 落在 {@code [300, 420)} 秒（随机抖动 → 防雪崩）；</li>
 *   <li>JSON 往返不丢字段（含 {@code LocalDateTime}、嵌套规格/SKU 列表）；</li>
 *   <li>空值标记：{@code NULL} + 60s（防穿透）；</li>
 *   <li>互斥锁：{@code setnx} 只放行一个（防击穿），Lua 释放只删自己的锁。</li>
 * </ul>
 *
 * <p>依赖本地 Redis 已启动（与运行应用一致）；用到的 key 在 {@link #clean()} 里清理。
 */
@SpringBootTest
@DisplayName("集成：商品详情缓存三防（真实 Redis）")
class MallRedisCacheClientIntegrationTest {

    private static final String KEY = MallRedisKeyConstant.PRODUCT_CACHE_PREFIX + "it-cache-test";
    private static final String LOCK_KEY = MallRedisKeyConstant.CACHE_LOCK_PREFIX + KEY;

    @Autowired
    private MallRedisCacheClient cacheClient;
    @Autowired
    private StringRedisTemplate redis;

    @AfterEach
    void clean() {
        redis.delete(List.of(KEY, LOCK_KEY));
    }

    private MallProductVO vo() {
        return MallProductVO.builder()
                .id(999999L)
                .categoryId(4L)
                .categoryName("T恤")
                .brandId(1L)
                .brandName("超级商城")
                .name("纯棉圆领T恤")
                .subtitle("260g 重磅纯棉")
                .mainImage("https://example.com/a.png")
                .priceLow(new BigDecimal("79.00"))
                .priceHigh(new BigDecimal("89.00"))
                .stock(100)
                .sales(7)
                .rating(new BigDecimal("4.50"))
                .reviewCount(2)
                .goodRate(new BigDecimal("50.00"))
                .status(1)
                .createTime(LocalDateTime.of(2026, 9, 22, 10, 30, 0))
                .updateTime(LocalDateTime.of(2026, 9, 22, 11, 0, 0))
                .detail("100% 精梳棉")
                .images(List.of("https://example.com/a.png", "https://example.com/b.png"))
                .specs(List.of(
                        MallSpecVO.builder().name("颜色").values(List.of("白", "黑")).build(),
                        MallSpecVO.builder().name("尺码").values(List.of("M", "L")).build()))
                .skus(List.of(
                        MallSkuVO.builder().id(1L).skuCode("TS-W-M").price(new BigDecimal("79.00"))
                                .stock(50).sales(3).status(1).specValues(List.of("白", "M")).build(),
                        MallSkuVO.builder().id(2L).skuCode("TS-B-L").price(new BigDecimal("89.00"))
                                .stock(50).sales(4).status(1).specValues(List.of("黑", "L")).build()))
                .build();
    }

    @Test
    @DisplayName("写 JSON 缓存 + 随机 TTL（300 + [0,120) 秒）")
    void setJson_writesWithJitteredTtl() {
        cacheClient.setJson(KEY, vo(), 300, 120);

        String json = redis.opsForValue().get(KEY);
        assertThat(json).isNotNull().contains("纯棉圆领T恤");

        Long ttl = redis.getExpire(KEY);
        assertThat(ttl)
                .as("TTL 必须落在 [300, 420) 秒，证明随机抖动生效（防雪崩）")
                .isGreaterThanOrEqualTo(300L)
                .isLessThan(420L);

        // 多次写入，TTL 不应完全相同（随机抖动的直接证据）
        Long first = ttl;
        boolean sawDifferent = false;
        for (int i = 0; i < 5 && !sawDifferent; i++) {
            cacheClient.setJson(KEY, vo(), 300, 120);
            sawDifferent = !redis.getExpire(KEY).equals(first);
        }
        assertThat(sawDifferent).as("5 次写入里至少出现一个不同的 TTL").isTrue();
    }

    @Test
    @DisplayName("JSON 往返：所有字段（含时间、嵌套规格/SKU）都能正确还原")
    void jsonRoundTrip_keepsAllFields() {
        MallProductVO origin = vo();
        cacheClient.setJson(KEY, origin, 300, 120);

        MallProductVO back = cacheClient.getJson(KEY, new TypeReference<>() {
        });

        assertThat(back).isNotNull();
        assertThat(back.getId()).isEqualTo(origin.getId());
        assertThat(back.getName()).isEqualTo(origin.getName());
        assertThat(back.getPriceLow()).isEqualByComparingTo(origin.getPriceLow());
        assertThat(back.getGoodRate()).isEqualByComparingTo(origin.getGoodRate());
        // LocalDateTime 往返（fastjson 默认写成字符串，读回来要能解析）
        assertThat(back.getCreateTime()).isEqualTo(origin.getCreateTime());
        assertThat(back.getUpdateTime()).isEqualTo(origin.getUpdateTime());
        assertThat(back.getImages()).containsExactlyElementsOf(origin.getImages());
        assertThat(back.getSpecs()).containsExactlyElementsOf(origin.getSpecs());
        assertThat(back.getSkus()).containsExactlyElementsOf(origin.getSkus());
    }

    @Test
    @DisplayName("防穿透：空值标记 NULL + 60s，读到它 getJson 返回 null 且 isNullMarked 为 true")
    void nullMark() {
        cacheClient.setNull(KEY, 60);

        assertThat(redis.opsForValue().get(KEY)).isEqualTo(MallRedisCacheClient.NULL_MARK);
        assertThat(cacheClient.isNullMarked(KEY)).isTrue();
        assertThat(cacheClient.getJson(KEY, new TypeReference<MallProductVO>() {
        })).isNull();
        assertThat(redis.getExpire(KEY)).isBetween(1L, 60L);
    }

    @Test
    @DisplayName("防击穿：互斥锁只放行一个；锁 key 带 mall:cache:lock: 前缀")
    void lock_onlyOneHolder() {
        assertThat(cacheClient.tryLock(KEY, 10)).isTrue();
        assertThat(cacheClient.tryLock(KEY, 10)).as("同一 key 第二次抢锁必须失败").isFalse();
        assertThat(redis.hasKey(LOCK_KEY)).isTrue();

        cacheClient.unlock(KEY);
        assertThat(redis.hasKey(LOCK_KEY)).isFalse();
        // 释放后可以重新抢
        assertThat(cacheClient.tryLock(KEY, 10)).isTrue();
    }

    @Test
    @DisplayName("释放锁是「值匹配才删」：别人的锁不会被误删（Lua 保证）")
    void unlock_onlyRemovesOwnLock() {
        // 模拟锁被别人（另一个线程）持有
        redis.opsForValue().set(LOCK_KEY, "another-thread");

        cacheClient.unlock(KEY);

        assertThat(redis.opsForValue().get(LOCK_KEY))
                .as("值不匹配时不能删除，否则会误删别人正在用的锁")
                .isEqualTo("another-thread");
    }

    @Test
    @DisplayName("删缓存：delete 能把详情缓存与空值缓存一起清掉")
    void delete_removesKey() {
        cacheClient.setJson(KEY, vo(), 300, 120);
        assertThat(redis.hasKey(KEY)).isTrue();

        cacheClient.delete(KEY);

        assertThat(redis.hasKey(KEY)).isFalse();
        assertThat(cacheClient.getJson(KEY, new TypeReference<MallProductVO>() {
        })).isNull();
    }
}
