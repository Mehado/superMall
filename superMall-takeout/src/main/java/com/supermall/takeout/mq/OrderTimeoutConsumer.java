package com.supermall.takeout.mq;

import com.rabbitmq.client.Channel;
import com.supermall.takeout.config.RabbitMQConfig;
import com.supermall.takeout.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 延迟关单消费者（幂等重试设计）。
 * <p>
 * 幂等：只有「待支付」才关单（条件更新）；
 * 失败按 retryCount 重投到重试队列，5 秒后再次回到死信队列，超过上限丢弃并告警，
 * 最终由 OrderTask 定时扫库兜底。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderService orderService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = {RabbitMQConfig.ORDER_DEAD_QUEUE})
    public void handleTimeoutOrder(Long orderId, Channel channel, Message message) throws Exception {
        Object retryCountObj = message.getMessageProperties().getHeader("retryCount");
        int retryCount = retryCountObj instanceof Number ? ((Number) retryCountObj).intValue() : 0;
        try {
            orderService.cancelTimeoutOrder(orderId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("延迟关单处理失败：orderId={}, retryCount={}", orderId, retryCount, e);
            if (retryCount < RabbitMQConfig.MAX_RETRY) {
                try {
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.ORDER_RETRY_EXCHANGE,
                            RabbitMQConfig.ORDER_RETRY_ROUTING_KEY,
                            orderId,
                            msg -> {
                                msg.getMessageProperties().setHeader("retryCount", retryCount + 1);
                                msg.getMessageProperties().setExpiration("5000");
                                msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                                return msg;
                            });
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    return;
                } catch (Exception republishEx) {
                    log.error("延迟关单重投失败，转定时任务兜底：orderId={}", orderId, republishEx);
                }
            } else {
                log.error("延迟关单超过最大重试次数，需人工介入：orderId={}", orderId);
            }
            // 统一出口：保证每条消息最终有且只有一个 ack/nack，绝不悬挂
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }
}
