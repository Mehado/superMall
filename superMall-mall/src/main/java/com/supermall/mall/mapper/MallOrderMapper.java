package com.supermall.mall.mapper;

import com.github.pagehelper.Page;
import com.supermall.mall.dto.MallOrderPageQueryDTO;
import com.supermall.mall.entity.MallOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单（mall_order）。
 */
@Mapper
public interface MallOrderMapper {

    void insert(MallOrder order);

    MallOrder getById(@Param("id") Long id);

    MallOrder getByNumber(@Param("orderNumber") String orderNumber);

    /** 下单幂等兜底：唯一键 (user_id, submit_token) */
    int countByUserAndToken(@Param("userId") Long userId, @Param("token") String token);

    /** 分页（买家传 userId，后台传 status/orderNumber/userId） */
    Page<MallOrder> page(MallOrderPageQueryDTO query);

    /**
     * 条件状态流转：仅当当前状态 = from 时才改为 to。
     * 返回 0 表示状态已变（并发重复流转），调用方据此报错，天然幂等。
     */
    int transitionStatus(@Param("id") Long id,
                         @Param("from") String from,
                         @Param("to") String to,
                         @Param("update") MallOrder update);

    /** 取消：仅当还是待付款 */
    int cancelIfPendingPayment(@Param("id") Long id,
                               @Param("now") LocalDateTime now,
                               @Param("reason") String reason);

    /** 支付成功：仅当待付款时置为待发货 */
    int markPaidIfPending(@Param("orderNumber") String orderNumber,
                          @Param("payTime") LocalDateTime payTime,
                          @Param("payMethod") String payMethod);

    /** 超时未支付：expire_time < deadline 且仍是待付款 */
    List<MallOrder> listTimeoutPending(@Param("deadline") LocalDateTime deadline);

    /** 按状态计数（后台看板用） */
    int countByStatus(@Param("status") String status);
}
