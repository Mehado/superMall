package com.supermall.takeout.mq;

import com.rabbitmq.client.Channel;
import com.supermall.takeout.config.RabbitMQConfig;
import com.supermall.takeout.entity.OrderItem;
import com.supermall.takeout.mapper.OrderItemMapper;
import com.supermall.takeout.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单完成异步统计消费者：订单完成后累加商品销量。
 * 把「统计」从主流程剥离，避免影响下单/支付的核心链路。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderStatConsumer {

    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    @RabbitListener(queues = {RabbitMQConfig.STAT_QUEUE})
    public void handleOrderCompleted(Long orderId, Channel channel, Message message) throws Exception {
        try {
            List<OrderItem> items = orderItemMapper.listByOrderId(orderId);
            for (OrderItem item : items) {
                productMapper.addSales(item.getProductId(), item.getNumber());
            }
            log.info("订单完成异步统计：orderId={}, 更新商品销量 {} 项", orderId, items.size());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("订单完成异步统计失败：orderId={}", orderId, e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
