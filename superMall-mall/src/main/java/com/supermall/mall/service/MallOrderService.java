package com.supermall.mall.service;

import com.supermall.mall.dto.MallOrderPageQueryDTO;
import com.supermall.mall.dto.MallOrderSubmitDTO;
import com.supermall.mall.dto.MallShipDTO;
import com.supermall.mall.entity.MallOrder;
import com.supermall.mall.vo.MallOrderSubmitVO;
import com.supermall.mall.vo.MallOrderVO;
import com.supermall.result.PageResult;

/**
 * 订单（买家侧 + 运营后台）。
 *
 * <p>关键约定：
 * <ul>
 *   <li>金额、商品、库存全部服务端算/扣，前端只能决定「用哪个地址、买购物车里哪些行」；</li>
 *   <li>所有状态流转都是条件更新（{@code where status = 期望值}），天然幂等；</li>
 *   <li>买家侧只能看/操作自己的订单（{@code userId} 从登录态取）。</li>
 * </ul>
 */
public interface MallOrderService {

    /** 下单幂等令牌（10 分钟有效，一次性） */
    String getSubmitToken();

    /** 从购物车「有效且选中」的行下单 */
    MallOrderSubmitVO submit(MallOrderSubmitDTO dto);

    /** 买家订单分页 */
    PageResult pageForUser(String status, Integer page, Integer pageSize);

    /** 买家订单详情（校验归属） */
    MallOrderVO detailForUser(String orderNumber);

    /** 买家取消未付款订单（回补库存） */
    void cancel(String orderNumber);

    /** 确认收货：待收货 → 已完成 */
    void confirmReceipt(String orderNumber);

    /**
     * 支付成功处理（条件更新 + 销量累加）。
     *
     * @return true 表示本次真正完成支付状态流转；false 表示订单已不是待付款（幂等跳过）
     */
    boolean paySuccess(String orderNumber, String payMethod);

    /** 超时未支付自动取消（定时任务调用，幂等） */
    void cancelTimeoutOrder(Long orderId);

    /* ---------------- 运营后台 ---------------- */

    PageResult pageForAdmin(MallOrderPageQueryDTO query);

    MallOrderVO detailForAdmin(String orderNumber);

    /** 发货：待发货 → 待收货，并写物流单 */
    void ship(String orderNumber, MallShipDTO dto);

    /** 取订单并校验归属（退款等场景复用） */
    MallOrder requireOwned(Long userId, String orderNumber);

    /** 订单实体 → VO（退款服务复用） */
    MallOrderVO toVO(MallOrder order);
}
