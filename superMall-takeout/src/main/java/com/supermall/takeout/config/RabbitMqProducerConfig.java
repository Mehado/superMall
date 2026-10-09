package com.supermall.takeout.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 生产者确认配置：
 * confirm 回调 = Broker 是否收到消息；returns 回调 = 消息是否成功路由到队列。
 */
@Configuration
@Slf4j
public class RabbitMqProducerConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("MQ 发送失败：correlationId={}, cause={}",
                        correlationData == null ? "null" : correlationData.getId(), cause);
            } else {
                log.debug("MQ 发送成功：correlationId={}",
                        correlationData == null ? "null" : correlationData.getId());
            }
        });

        template.setReturnsCallback(returned ->
                log.error("MQ 路由失败：exchange={}, routingKey={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        return template;
    }
}
