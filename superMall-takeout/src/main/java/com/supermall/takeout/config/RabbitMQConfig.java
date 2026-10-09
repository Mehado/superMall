package com.supermall.takeout.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置：延迟关单队列，并含支付成功 / 异步统计队列。
 *
 * 1. 订单超时取消：延迟队列 TTL=15min，消息过期 -> 死信交换机 -> 关单队列 -> 消费者关单
 * 2. 支付成功异步处理：pay.success.exchange -> pay.success.queue
 * 3. 异步统计：order.completed.exchange -> order.completed.queue（更新销量）
 */
@Configuration
public class RabbitMQConfig {

    /* ---------------- 订单超时取消 ---------------- */
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";
    public static final String ORDER_DEAD_EXCHANGE = "order.dead.exchange";
    public static final String ORDER_DEAD_QUEUE = "order.dead.queue";
    public static final String ORDER_DEAD_ROUTING_KEY = "order.dead";
    public static final String ORDER_RETRY_EXCHANGE = "order.retry.exchange";
    public static final String ORDER_RETRY_QUEUE = "order.retry.queue";
    public static final String ORDER_RETRY_ROUTING_KEY = "order.retry";

    /* ---------------- 支付成功异步 ---------------- */
    public static final String PAY_EXCHANGE = "pay.success.exchange";
    public static final String PAY_QUEUE = "pay.success.queue";
    public static final String PAY_ROUTING_KEY = "pay.success";

    /* ---------------- 订单完成异步统计 ---------------- */
    public static final String STAT_EXCHANGE = "order.stat.exchange";
    public static final String STAT_QUEUE = "order.stat.queue";
    public static final String STAT_ROUTING_KEY = "order.stat";

    /** 消费失败最大重试次数 */
    public static final int MAX_RETRY = 3;

    /** 延迟关单 TTL：15 分钟（毫秒） */
    public static final int ORDER_DELAY_TTL = 15 * 60 * 1000;

    /* ---------------- 订单超时取消 ---------------- */

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", ORDER_DELAY_TTL);
        args.put("x-dead-letter-exchange", ORDER_DEAD_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_DEAD_ROUTING_KEY);
        return new Queue(ORDER_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public DirectExchange orderDeadExchange() {
        return new DirectExchange(ORDER_DEAD_EXCHANGE);
    }

    @Bean
    public Queue orderDeadQueue() {
        return new Queue(ORDER_DEAD_QUEUE, true);
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderExchange()).with(ORDER_DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding orderDeadBinding() {
        return BindingBuilder.bind(orderDeadQueue()).to(orderDeadExchange()).with(ORDER_DEAD_ROUTING_KEY);
    }

    /** 重试交换机：消费失败的消息发到这里，5 秒后重新投递 */
    @Bean
    public DirectExchange orderRetryExchange() {
        return new DirectExchange(ORDER_RETRY_EXCHANGE);
    }

    @Bean
    public Queue orderRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 5000);
        args.put("x-dead-letter-exchange", ORDER_DEAD_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_DEAD_ROUTING_KEY);
        return new Queue(ORDER_RETRY_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding orderRetryBinding() {
        return BindingBuilder.bind(orderRetryQueue()).to(orderRetryExchange()).with(ORDER_RETRY_ROUTING_KEY);
    }

    /* ---------------- 支付成功异步 ---------------- */

    @Bean
    public DirectExchange payExchange() {
        return new DirectExchange(PAY_EXCHANGE);
    }

    @Bean
    public Queue payQueue() {
        return new Queue(PAY_QUEUE, true);
    }

    @Bean
    public Binding payBinding() {
        return BindingBuilder.bind(payQueue()).to(payExchange()).with(PAY_ROUTING_KEY);
    }

    /* ---------------- 订单完成异步统计 ---------------- */

    @Bean
    public DirectExchange statExchange() {
        return new DirectExchange(STAT_EXCHANGE);
    }

    @Bean
    public Queue statQueue() {
        return new Queue(STAT_QUEUE, true);
    }

    @Bean
    public Binding statBinding() {
        return BindingBuilder.bind(statQueue()).to(statExchange()).with(STAT_ROUTING_KEY);
    }
}
