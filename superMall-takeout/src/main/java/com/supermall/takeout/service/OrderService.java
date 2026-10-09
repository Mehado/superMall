package com.supermall.takeout.service;

import com.supermall.takeout.dto.OrderPageQueryDTO;
import com.supermall.takeout.dto.OrderSubmitDTO;
import com.supermall.result.PageResult;
import com.supermall.takeout.vo.OrderStatisticsVO;
import com.supermall.takeout.vo.OrderSubmitVO;
import com.supermall.takeout.vo.OrderVO;

public interface OrderService {

    /* ---------------- 用户端 ---------------- */

    /** 获取下单幂等令牌 */
    String getSubmitToken();

    /** 用户提交订单（按商家拆单，扣库存，清购物车，发延迟关单消息） */
    OrderSubmitVO submitOrder(OrderSubmitDTO dto);

    /** 用户订单分页 */
    PageResult pageQuery4User(int page, int pageSize, String status);

    /** 用户订单详情（校验归属） */
    OrderVO details(Long id);

    /** 用户取消订单 */
    void userCancel(Long id);

    /* ---------------- 商家端 ---------------- */

    /** 商家订单分页（merchantId 隔离） */
    PageResult pageQuery4Merchant(OrderPageQueryDTO dto);

    /** 商家订单详情 */
    OrderVO merchantDetails(Long id);

    /** 商家接单：PAID -> MERCHANT_ACCEPTED */
    void merchantAccept(Long id);

    /** 备餐完成：MERCHANT_ACCEPTED -> WAITING_DELIVERY，并生成配送单 */
    void merchantReady(Long id);

    /** 商家拒单：PAID -> CANCELLED，回补库存 */
    void merchantReject(Long id, String reason);

    /** 商家订单状态统计 */
    OrderStatisticsVO statistics();

    /* ---------------- 支付 ---------------- */

    /** 支付成功（幂等）：待支付 -> 已支付，发支付成功 MQ */
    void paySuccess(String orderNumber, String payMethod);

    /* ---------------- 配送联动 ---------------- */

    /** 配送员取餐：WAITING_DELIVERY -> DELIVERING */
    void markDelivering(Long orderId);

    /** 确认送达：DELIVERING -> COMPLETED，发异步统计 MQ */
    void markCompleted(Long orderId);

    /* ---------------- 超时兜底 ---------------- */

    /** 超时未支付关单（MQ / 定时任务共用，条件更新保证幂等） */
    void cancelTimeoutOrder(Long orderId);
}
