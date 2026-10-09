package com.supermall.mall.service;

import com.supermall.mall.dto.MallPaymentDTO;
import com.supermall.mall.vo.MallPaymentResultVO;

/**
 * 支付（当前为 Mock，点一下即成功）。
 *
 * <p>用 Redis 一次性令牌 + 订单条件更新保证支付幂等；
 * 将来接支付宝沙箱（P8）只需换掉 {@code pay} 的实现，接口形态不变。
 */
public interface MallPaymentService {

    /** 获取支付令牌（5 分钟有效，一次性） */
    String getPayToken(String orderNumber);

    /** 发起支付（Mock 直接成功） */
    MallPaymentResultVO pay(MallPaymentDTO dto);
}
