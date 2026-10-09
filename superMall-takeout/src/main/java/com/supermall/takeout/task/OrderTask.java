package com.supermall.takeout.task;

import com.supermall.takeout.entity.Orders;
import com.supermall.takeout.enumeration.OrderStatus;
import com.supermall.takeout.mapper.OrderMapper;
import com.supermall.takeout.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务：超时未支付兜底关单。
 * MQ 延迟队列已覆盖绝大多数场景，这里作为最后一道保险（MQ 故障 / 重试耗尽）。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderTask {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Value("${super-mall.delivery.pay-timeout-minutes:15}")
    private long payTimeoutMinutes;

    /** 每分钟扫描一次超时未支付订单 */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(payTimeoutMinutes);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(
                OrderStatus.PENDING_PAYMENT.name(), deadline);
        if (ordersList == null || ordersList.isEmpty()) {
            return;
        }
        log.info("定时兜底关单：待处理 {} 单", ordersList.size());
        for (Orders orders : ordersList) {
            orderService.cancelTimeoutOrder(orders.getId());
        }
    }
}
