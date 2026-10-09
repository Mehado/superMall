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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发测试：同一个下单/支付令牌被多线程同时消费，只允许一个成功（幂等，防重复下单）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("并发：幂等令牌只被消费一次")
class IdempotentTokenConcurrencyTest {

    private static final int THREADS = 50;

    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    @Qualifier("idempotentTokenScript")
    private DefaultRedisScript<Long> idempotentTokenScript;

    @AfterEach
    void tearDown() {
        redis.delete(RedisKeyConstant.SUBMIT_TOKEN_PREFIX + "concurrency-token-1");
    }

    @Test
    @DisplayName("50 线程消费同一令牌：恰好 1 次成功，令牌被删除")
    void onlyOneThreadConsumesTheToken() throws Exception {
        String key = RedisKeyConstant.SUBMIT_TOKEN_PREFIX + "concurrency-token-1";
        redis.opsForValue().set(key, "1001");

        ConcurrentRunner.Result<Long> result = ConcurrentRunner.run(THREADS, 30,
                () -> redis.execute(idempotentTokenScript, Collections.singletonList(key), "1001"));

        assertThat(result.finished()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.count(v -> v != null && v == 1L)).as("成功消费次数").isEqualTo(1);
        assertThat(result.count(v -> v != null && v == 0L)).as("重复提交次数").isEqualTo(THREADS - 1);
        assertThat(redis.hasKey(key)).as("令牌应被删除").isFalse();
    }
}
