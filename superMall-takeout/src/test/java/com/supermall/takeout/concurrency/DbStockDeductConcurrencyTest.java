package com.supermall.takeout.concurrency;

import com.supermall.takeout.entity.Product;
import com.supermall.takeout.mapper.ProductMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发测试：数据库层条件扣减（stock &gt;= n）作为防超卖的第二道防线。
 * <p>
 * 与 Redis 预扣不同，这里直接并发打 DB，验证行锁 + 条件更新不会把库存扣成负数。
 * 测试数据自建自删，不依赖已有商品。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("并发：DB 条件扣减不超卖")
class DbStockDeductConcurrencyTest {

    private static final int THREADS = 100;
    private static final int STOCK = 10;
    private static final Long MERCHANT_ID = 1L;

    @Autowired
    private ProductMapper productMapper;

    private Long productId;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        Product product = Product.builder()
                .merchantId(MERCHANT_ID)
                .categoryId(1L)
                .businessType(1)
                .name("并发测试-库存商品")
                .price(new BigDecimal("1.00"))
                .stock(STOCK)
                .sales(0)
                .status(0) // 下架，避免出现在前台/库存预热里
                .createTime(now)
                .updateTime(now)
                .build();
        productMapper.insert(product);
        productId = product.getId();
        assertThat(productId).isNotNull();
    }

    @AfterEach
    void tearDown() {
        if (productId != null) {
            productMapper.deleteById(productId, MERCHANT_ID);
        }
    }

    @Test
    @DisplayName("100 线程扣 10 件库存：恰好 10 次成功，库存归零不为负")
    void onlyTenOfHundredThreadsDeductSuccessfully() throws Exception {
        ConcurrentRunner.Result<Integer> result = ConcurrentRunner.run(THREADS, 30,
                () -> productMapper.deductStock(productId, 1));

        assertThat(result.finished()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.count(v -> v != null && v == 1)).as("扣减成功次数").isEqualTo(STOCK);
        assertThat(result.count(v -> v != null && v == 0)).as("库存不足次数").isEqualTo(THREADS - STOCK);
        assertThat(productMapper.getById(productId).getStock()).as("最终库存").isZero();
    }
}
