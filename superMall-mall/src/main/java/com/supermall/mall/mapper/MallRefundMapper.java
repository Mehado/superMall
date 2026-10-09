package com.supermall.mall.mapper;

import com.github.pagehelper.Page;
import com.supermall.mall.entity.MallRefund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款申请（mall_refund）。
 */
@Mapper
public interface MallRefundMapper {

    void insert(MallRefund refund);

    MallRefund getById(@Param("id") Long id);

    /** 一单最多一个退款申请（申请时订单已置为 REFUNDING，天然互斥） */
    MallRefund getByOrderId(@Param("orderId") Long orderId);

    /** 批量取这些订单的退款单（一单可能有多条历史申请，调用方按 id 最大保留最新一条） */
    List<MallRefund> listByOrderIds(@Param("orderIds") List<Long> orderIds);

    Page<MallRefund> page(@Param("status") String status);

    /**
     * 审核：仅当还是 PENDING 时才更新，防并发重复审核。
     */
    int audit(@Param("id") Long id,
              @Param("status") String status,
              @Param("adminRemark") String adminRemark,
              @Param("auditTime") LocalDateTime auditTime);

    int countByStatus(@Param("status") String status);
}
