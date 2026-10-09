package com.supermall.takeout.service;

import com.supermall.takeout.dto.DeliveryPageQueryDTO;
import com.supermall.result.PageResult;
import com.supermall.takeout.vo.DeliveryOrderVO;
import com.supermall.takeout.vo.DeliveryStatisticsVO;

public interface DeliveryService {

    /** 待抢单池 */
    PageResult pool(DeliveryPageQueryDTO dto);

    /** 抢单：Redis + Lua 闸门 + DB 条件更新，保证只有一个配送员成功 */
    void grab(Long orderId);

    /** 我的配送单 */
    PageResult myOrders(DeliveryPageQueryDTO dto);

    /** 配送单详情 */
    DeliveryOrderVO details(Long orderId);

    /** 取餐：ACCEPTED -> DELIVERING，同步订单为配送中 */
    void pickup(Long orderId);

    /** 确认送达：DELIVERING -> DELIVERED，同步订单为已完成 */
    void delivered(Long orderId);

    /** 配送端统计 */
    DeliveryStatisticsVO statistics();
}
