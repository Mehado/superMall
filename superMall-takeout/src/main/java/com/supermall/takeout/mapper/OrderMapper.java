package com.supermall.takeout.mapper;

import com.github.pagehelper.Page;
import com.supermall.takeout.dto.OrderPageQueryDTO;
import com.supermall.takeout.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    void insert(Orders orders);

    Orders getById(Long id);

    Orders getByNumber(String number);

    void update(Orders orders);

    Page<Orders> pageQuery(OrderPageQueryDTO dto);

    int countByUserAndToken(@Param("userId") Long userId, @Param("token") String token);

    int countByStatus(@Param("merchantId") Long merchantId, @Param("status") String status);

    List<Orders> getByStatusAndOrderTimeLT(@Param("status") String status,
                                           @Param("orderTime") LocalDateTime orderTime);

    /** 仅当还是待付款时取消，返回影响行数（防并发双回补） */
    int cancelByIdIfPendingPayment(@Param("id") Long id,
                                   @Param("now") LocalDateTime now,
                                   @Param("reason") String reason);

    /** 条件状态流转：仅当处于期望状态时才更新 */
    int transitionStatus(@Param("id") Long id,
                         @Param("from") String from,
                         @Param("to") String to,
                         @Param("orders") Orders orders);

    /** 支付成功：仅当待付款时置为已支付 */
    int markPaidIfPending(@Param("orderNumber") String orderNumber,
                          @Param("checkoutTime") LocalDateTime checkoutTime,
                          @Param("payMethod") String payMethod);

    /** 商家维度营业额 */
    Double sumAmountByMerchant(@Param("merchantId") Long merchantId,
                               @Param("begin") LocalDateTime begin,
                               @Param("end") LocalDateTime end);

    /** 商家维度有效订单数（时间区间内，排除已取消与待付款） */
    int countByMerchantAndTime(@Param("merchantId") Long merchantId,
                               @Param("begin") LocalDateTime begin,
                               @Param("end") LocalDateTime end);
}
