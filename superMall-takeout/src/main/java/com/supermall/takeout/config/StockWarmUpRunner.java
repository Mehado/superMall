package com.supermall.takeout.config;

import com.supermall.takeout.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动完成后把 DB 库存预热到 Redis。
 * Redis 未启动时不阻断启动，下单时会提示「库存未预热」。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockWarmUpRunner implements ApplicationRunner {

    private final StockService stockService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            stockService.warmUpAll();
        } catch (Exception e) {
            log.warn("库存预热失败（Redis 未启动？），下单时将提示「库存未预热」", e);
        }
    }
}
