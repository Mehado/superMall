package com.supermall.takeout.mapper;

import com.github.pagehelper.Page;
import com.supermall.takeout.dto.DeliveryPageQueryDTO;
import com.supermall.takeout.entity.DeliveryOrder;
import com.supermall.takeout.vo.DeliveryOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface DeliveryOrderMapper {

    void insert(DeliveryOrder deliveryOrder);

    DeliveryOrder getById(Long id);

    DeliveryOrder getByOrderId(Long orderId);

    void update(DeliveryOrder deliveryOrder);

    /** 抢占配送单：仅当 WAITING 时把 deliverymanId / status 写入，返回影响行数 */
    int acceptIfWaiting(@Param("orderId") Long orderId,
                        @Param("deliverymanId") Long deliverymanId,
                        @Param("now") LocalDateTime now);

    /** 配送端抢单池：WAITING 的配送单 */
    Page<DeliveryOrderVO> pagePool(DeliveryPageQueryDTO dto);

    /** 我的配送单 */
    Page<DeliveryOrderVO> pageByDeliveryman(@Param("deliverymanId") Long deliverymanId,
                                            @Param("dto") DeliveryPageQueryDTO dto);

    int countPoolWaiting();

    int countByDeliverymanAndStatus(@Param("deliverymanId") Long deliverymanId,
                                    @Param("status") String status);
}
