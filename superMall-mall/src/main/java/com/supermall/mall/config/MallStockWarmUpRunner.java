package com.supermall.mall.config;

import com.supermall.mall.service.MallStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动后把 DB 里的 SKU 库存预热到 Redis。
 *
 * <p>Redis 没起来时不阻断启动：下单会提示「库存未预热」，
 * 卖家改商品时也会通过 {@code syncProductSkuStock} 重新同步。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MallStockWarmUpRunner implements ApplicationRunner {

    private final MallStockService mallStockService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            mallStockService.warmUpAll();
        } catch (Exception e) {
            log.warn("[mall] 库存预热失败（Redis 未启动？），下单时会提示「库存未预热」", e);
        }
    }
}
