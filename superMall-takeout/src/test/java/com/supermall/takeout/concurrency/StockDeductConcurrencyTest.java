package com.supermall.takeout.concurrency;

import com.supermall.constant.MessageConstant;
import com.supermall.takeout.constant.RedisKeyConstant;
import com.supermall.takeout.entity.CartItem;
import com.supermall.takeout.exception.OrderBusinessException;
import com.supermall.takeout.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发测试（核心）：100 个线程抢 10 件库存，验证 Redis + Lua 的原子扣减不会超卖。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("并发：库存预扣不超卖")
class StockDeductConcurrencyTest {

    private static final int THREADS = 100;
    private static final int STOCK = 10;
    private static final Long PRODUCT_ID = 999_999L;
    private static final String KEY = RedisKeyConstant.PRODUCT_STOCK_PREFIX + PRODUCT_ID;

    @Autowired
    private StockService stockService;
    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        redis.opsForValue().set(KEY, String.valueOf(STOCK));
    }

    @AfterEach
    void tearDown() {
        redis.delete(KEY);
    }

    @Test
    @DisplayName("100 线程抢 10 件：恰好 10 次成功、90 次库存不足、余额不为负")
    void onlyTenOfHundredThreadsSucceed() throws Exception {
        CartItem oneItem = CartItem.builder()
                .productId(PRODUCT_ID)
                .skuId(0L)
                .number(1)
                .build();

        ConcurrentRunner.Result<Boolean> result = ConcurrentRunner.run(THREADS, 30, () -> {
            stockService.deductStock(List.of(oneItem));
            return Boolean.TRUE;
        });

        assertThat(result.finished()).as("并发任务应在超时前全部完成").isTrue();
        assertThat(result.errors()).as("失败原因都应是库存不足").allSatisfy(e ->
                assertThat(e).isInstanceOf(OrderBusinessException.class)
                        .hasMessage(MessageConstant.STOCK_NOT_ENOUGH));
        assertThat(result.values()).as("成功次数").hasSize(STOCK);
        assertThat(result.errors()).as("失败次数").hasSize(THREADS - STOCK);
        assertThat(redis.opsForValue().get(KEY)).as("最终库存").isEqualTo("0");
    }

    @Test
    @DisplayName("未预热库存：扣减直接失败，不会写出负数")
    void notWarmedUp_isRejected() {
        redis.delete(KEY);

        assertThat(resultOfDeduct()).isNull();
        assertThat(redis.opsForValue().get(KEY)).isNull();
    }

    private Boolean resultOfDeduct() {
        try {
            stockService.deductStock(List.of(CartItem.builder()
                    .productId(PRODUCT_ID).skuId(0L).number(1).build()));
            return Boolean.TRUE;
        } catch (OrderBusinessException e) {
            assertThat(e).hasMessage(MessageConstant.STOCK_NOT_WARMED);
            return null;
        }
    }
}
