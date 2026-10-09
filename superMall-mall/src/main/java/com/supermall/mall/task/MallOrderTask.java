package com.supermall.mall.task;

import com.supermall.mall.entity.MallOrder;
import com.supermall.mall.mapper.MallOrderMapper;
import com.supermall.mall.service.MallOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务：每分钟扫描超时未支付的订单并自动取消。
 *
 * <p>电商不接 MQ（外卖那边用延迟队列 + 定时任务双保险），
 * 需求里超时取消只要求定时任务，所以这里就是唯一的一道。
 * 取消走的是同一个 {@code cancelTimeoutOrder}（条件更新，幂等），
 * 买家在这一分钟内自己取消也不会重复回补库存。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MallOrderTask {

    private final MallOrderMapper mallOrderMapper;
    private final MallOrderService mallOrderService;

    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        List<MallOrder> orders = mallOrderMapper.listTimeoutPending(LocalDateTime.now());
        if (orders == null || orders.isEmpty()) {
            return;
        }
        log.info("[mall] 超时未支付订单：待取消 {} 单", orders.size());
        for (MallOrder order : orders) {
            try {
                mallOrderService.cancelTimeoutOrder(order.getId());
            } catch (Exception e) {
                // 单个订单失败不影响其它订单
                log.error("[mall] 超时关单失败：orderId={}", order.getId(), e);
            }
        }
    }
}
