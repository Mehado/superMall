package com.supermall.takeout.service;

import com.supermall.takeout.dto.PaymentDTO;
import com.supermall.takeout.vo.OrderPaymentVO;

/**
 * 支付服务抽象：便于后续扩展微信 / 支付宝。
 * <pre>
 * PaymentService
 * ├── MockPaymentService
 * ├── WechatPaymentService（预留）
 * └── AlipayPaymentService（预留）
 * </pre>
 */
public interface PaymentService {

    /** 获取支付幂等令牌 */
    String getPayToken(String orderNumber);

    /** 发起支付（开发阶段为 Mock 支付） */
    OrderPaymentVO pay(PaymentDTO dto);
}
