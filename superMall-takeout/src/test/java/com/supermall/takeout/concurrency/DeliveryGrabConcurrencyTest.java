package com.supermall.takeout.concurrency;

import com.supermall.takeout.constant.RedisKeyConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发测试：50 个配送员同时抢同一张配送单，只允许一个抢到（Redis 闸门 + Lua 原子）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("并发：配送单只被一个骑手抢到")
class DeliveryGrabConcurrencyTest {

    private static final int THREADS = 50;
    private static final Long ORDER_ID = 888_888L;
    private static final String KEY = RedisKeyConstant.DELIVERY_GRAB_PREFIX + ORDER_ID;

    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    @Qualifier("grabDeliveryScript")
    private DefaultRedisScript<Long> grabDeliveryScript;

    @AfterEach
    void tearDown() {
        redis.delete(KEY);
    }

    @Test
    @DisplayName("50 骑手抢同一单：恰好 1 人成功")
    void onlyOneRiderWinsTheGrab() throws Exception {
        redis.delete(KEY);
        AtomicInteger riderSeq = new AtomicInteger(1000);

        ConcurrentRunner.Result<Long> result = ConcurrentRunner.run(THREADS, 30, () -> {
            String riderId = String.valueOf(riderSeq.incrementAndGet());
            return redis.execute(grabDeliveryScript, Collections.singletonList(KEY), riderId, "60");
        });

        assertThat(result.finished()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.count(v -> v != null && v == 1L)).as("抢单成功人数").isEqualTo(1);
        assertThat(result.count(v -> v != null && v == 0L)).as("手慢人数").isEqualTo(THREADS - 1);
        assertThat(redis.opsForValue().get(KEY)).as("闸门应记录了获胜骑手").isNotBlank();
    }
}
