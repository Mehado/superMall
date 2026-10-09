package com.supermall.takeout.mq;

import com.rabbitmq.client.Channel;
import com.supermall.takeout.config.RabbitMQConfig;
import com.supermall.takeout.entity.OrderItem;
import com.supermall.takeout.mapper.OrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 支付成功异步消费者：演示「支付成功后异步处理订单相关业务」。
 * 当前实现为记录日志 + 预留给后续扩展（如发送短信、记账、通知商家）。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaySuccessConsumer {

    private final OrderItemMapper orderItemMapper;

    @RabbitListener(queues = {RabbitMQConfig.PAY_QUEUE})
    public void handlePaySuccess(Long orderId, Channel channel, Message message) throws Exception {
        try {
            List<OrderItem> items = orderItemMapper.listByOrderId(orderId);
            log.info("支付成功异步处理：orderId={}, 明细数={}", orderId, items.size());
            // TODO 可扩展：通知商家、记录流水、发短信等
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("支付成功异步处理失败：orderId={}", orderId, e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
