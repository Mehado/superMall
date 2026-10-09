package com.supermall.takeout.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.takeout.constant.RedisKeyConstant;
import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.PaymentDTO;
import com.supermall.takeout.entity.Orders;
import com.supermall.takeout.enumeration.OrderStatus;
import com.supermall.takeout.exception.PaymentBusinessException;
import com.supermall.takeout.mapper.OrderMapper;
import com.supermall.takeout.service.OrderService;
import com.supermall.takeout.service.PaymentService;
import com.supermall.takeout.vo.OrderPaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Mock 支付实现：不接入真实支付，点击即成功。
 * 通过 Redis 令牌 + 订单条件更新保证支付幂等。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MockPaymentService implements PaymentService {

    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final StringRedisTemplate stringRedisTemplate;
    /** 幂等令牌消费脚本 */
    private final DefaultRedisScript<Long> idempotentTokenScript;

    @Override
    public String getPayToken(String orderNumber) {
        Orders orders = orderMapper.getByNumber(orderNumber);
        if (orders == null || !BaseContext.getCurrentId().equals(orders.getUserId())) {
            throw new PaymentBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(orders.getStatus())) {
            throw new PaymentBusinessException(MessageConstant.PAY_ORDER_STATUS_ERROR);
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstant.PAY_TOKEN_PREFIX + token,
                String.valueOf(BaseContext.getCurrentId()),
                5, TimeUnit.MINUTES);
        return token;
    }

    @Override
    public OrderPaymentVO pay(PaymentDTO dto) {
        // 消费支付令牌，防重复支付
        Long consumed = stringRedisTemplate.execute(
                idempotentTokenScript,
                Collections.singletonList(RedisKeyConstant.PAY_TOKEN_PREFIX + dto.getPayToken()),
                String.valueOf(BaseContext.getCurrentId()));
        if (consumed == null || consumed != 1L) {
            throw new PaymentBusinessException(MessageConstant.PAY_TOKEN_INVALID);
        }

        // 模拟支付成功
        orderService.paySuccess(dto.getOrderNumber(), "MOCK");

        return OrderPaymentVO.builder()
                .nonceStr("mock_nonce")
                .paySign("mock_pay_sign")
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .signType("RSA")
                .packageStr("prepay_id=mock_prepay_id")
                .build();
    }
}
