package com.supermall.mall.service.impl;

import com.supermall.context.BaseContext;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.constant.MallRedisKeyConstant;
import com.supermall.mall.dto.MallPaymentDTO;
import com.supermall.mall.entity.MallOrder;
import com.supermall.mall.enumeration.MallOrderStatus;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallOrderMapper;
import com.supermall.mall.service.MallOrderService;
import com.supermall.mall.service.MallPaymentService;
import com.supermall.mall.vo.MallPaymentResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Mock 支付实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallPaymentServiceImpl implements MallPaymentService {

    private static final String MOCK = "MOCK";
    private static final int TOKEN_TTL_MINUTES = 5;

    private final MallOrderMapper mallOrderMapper;
    private final MallOrderService mallOrderService;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> mallIdempotentTokenScript;

    @Override
    public String getPayToken(String orderNumber) {
        MallOrder order = mallOrderMapper.getByNumber(orderNumber);
        if (order == null || !BaseContext.getCurrentId().equals(order.getUserId())) {
            throw new MallBusinessException(MallMessageConstant.ORDER_NOT_FOUND);
        }
        if (!MallOrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new MallBusinessException(MallMessageConstant.PAY_ORDER_STATUS_ERROR);
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                MallRedisKeyConstant.PAY_TOKEN_PREFIX + token,
                String.valueOf(BaseContext.getCurrentId()),
                TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    @Override
    public MallPaymentResultVO pay(MallPaymentDTO dto) {
        // 消费支付令牌（一次性，防重复支付）
        Long consumed = stringRedisTemplate.execute(
                mallIdempotentTokenScript,
                Collections.singletonList(MallRedisKeyConstant.PAY_TOKEN_PREFIX + dto.getPayToken()),
                String.valueOf(BaseContext.getCurrentId()));
        if (consumed == null || consumed != 1L) {
            throw new MallBusinessException(MallMessageConstant.PAY_TOKEN_INVALID);
        }

        boolean paid = mallOrderService.paySuccess(dto.getOrderNumber(), MOCK);
        if (!paid) {
            throw new MallBusinessException(MallMessageConstant.PAY_ORDER_STATUS_ERROR);
        }

        MallOrder order = mallOrderMapper.getByNumber(dto.getOrderNumber());
        return MallPaymentResultVO.builder()
                .orderNumber(dto.getOrderNumber())
                .payAmount(order == null ? null : order.getPayAmount())
                .payTime(order == null ? LocalDateTime.now() : order.getPayTime())
                .nonceStr("mock_nonce")
                .paySign("mock_pay_sign")
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .signType("RSA")
                .packageStr("prepay_id=mock_prepay_id")
                .build();
    }
}
