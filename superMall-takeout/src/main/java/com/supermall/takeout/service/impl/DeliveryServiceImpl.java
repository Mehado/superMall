package com.supermall.takeout.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.constant.MessageConstant;
import com.supermall.takeout.constant.RedisKeyConstant;
import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.DeliveryPageQueryDTO;
import com.supermall.takeout.entity.DeliveryOrder;
import com.supermall.takeout.entity.Merchant;
import com.supermall.takeout.entity.Orders;
import com.supermall.takeout.enumeration.DeliveryStatus;
import com.supermall.takeout.exception.DeliveryBusinessException;
import com.supermall.takeout.mapper.DeliveryOrderMapper;
import com.supermall.takeout.mapper.MerchantMapper;
import com.supermall.takeout.mapper.OrderMapper;
import com.supermall.result.PageResult;
import com.supermall.takeout.service.DeliveryService;
import com.supermall.takeout.service.OrderService;
import com.supermall.takeout.vo.DeliveryOrderVO;
import com.supermall.takeout.vo.DeliveryStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 配送服务实现。
 * <p>
 * 抢单并发控制：Redis + Lua 做「同一配送单只放行一个配送员」的闸门，
 * 再由 DB 条件更新（where status='WAITING'）做最终一致性保证；
 * 若闸门抢到但 DB 更新失败（极端并发），释放 Redis 标记，避免死锁。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private static final long GRAB_FLAG_TTL_SECONDS = 300;

    private final DeliveryOrderMapper deliveryOrderMapper;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final OrderService orderService;
    private final StringRedisTemplate stringRedisTemplate;
    /** 抢单闸门脚本：由 RedisScriptConfig 从 .lua 文件加载为 Bean */
    private final DefaultRedisScript<Long> grabDeliveryScript;

    @Override
    public PageResult pool(DeliveryPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<DeliveryOrderVO> page = deliveryOrderMapper.pagePool(dto);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional
    public void grab(Long orderId) {
        Long deliverymanId = BaseContext.getCurrentId();

        DeliveryOrder delivery = deliveryOrderMapper.getByOrderId(orderId);
        if (delivery == null) {
            throw new DeliveryBusinessException(MessageConstant.DELIVERY_ORDER_NOT_FOUND);
        }
        if (!DeliveryStatus.WAITING.name().equals(delivery.getStatus())) {
            throw new DeliveryBusinessException(MessageConstant.DELIVERY_ORDER_TAKEN);
        }

        // 1. Redis 闸门：同单只放行一个（Lua 原子）
        String grabKey = RedisKeyConstant.DELIVERY_GRAB_PREFIX + orderId;
        Long allowed = stringRedisTemplate.execute(
                grabDeliveryScript,
                Collections.singletonList(grabKey),
                String.valueOf(deliverymanId),
                String.valueOf(GRAB_FLAG_TTL_SECONDS));
        if (allowed == null || allowed != 1L) {
            throw new DeliveryBusinessException(MessageConstant.DELIVERY_ORDER_TAKEN);
        }

        // 2. DB 条件更新：仅当 WAITING 才成功
        int rows = deliveryOrderMapper.acceptIfWaiting(orderId, deliverymanId, LocalDateTime.now());
        if (rows == 0) {
            // 极端并发下闸门与 DB 不一致，释放闸门后抛出
            stringRedisTemplate.delete(grabKey);
            throw new DeliveryBusinessException(MessageConstant.DELIVERY_ORDER_TAKEN);
        }
        log.info("配送员 {} 抢单成功：orderId={}", deliverymanId, orderId);
    }

    @Override
    public PageResult myOrders(DeliveryPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<DeliveryOrderVO> page = deliveryOrderMapper.pageByDeliveryman(BaseContext.getCurrentId(), dto);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public DeliveryOrderVO details(Long orderId) {
        DeliveryOrder delivery = deliveryOrderMapper.getByOrderId(orderId);
        if (delivery == null) {
            throw new DeliveryBusinessException(MessageConstant.DELIVERY_ORDER_NOT_FOUND);
        }
        Long deliverymanId = BaseContext.getCurrentId();
        boolean isOwner = deliverymanId.equals(delivery.getDeliverymanId());
        boolean isWaiting = DeliveryStatus.WAITING.name().equals(delivery.getStatus());
        if (!isOwner && !isWaiting) {
            throw new DeliveryBusinessException(MessageConstant.DELIVERY_NO_PERMISSION);
        }
        return buildVO(delivery);
    }

    @Override
    @Transactional
    public void pickup(Long orderId) {
        DeliveryOrder delivery = requireMyDelivery(orderId, DeliveryStatus.ACCEPTED);
        deliveryOrderMapper.update(DeliveryOrder.builder()
                .id(delivery.getId())
                .status(DeliveryStatus.DELIVERING.name())
                .pickupTime(LocalDateTime.now())
                .build());
        // 同步订单状态为配送中
        orderService.markDelivering(orderId);
        log.info("配送员取餐，订单进入配送中：orderId={}", orderId);
    }

    @Override
    @Transactional
    public void delivered(Long orderId) {
        DeliveryOrder delivery = requireMyDelivery(orderId, DeliveryStatus.DELIVERING);
        deliveryOrderMapper.update(DeliveryOrder.builder()
                .id(delivery.getId())
                .status(DeliveryStatus.DELIVERED.name())
                .deliveredTime(LocalDateTime.now())
                .build());
        // 同步订单状态为已完成（内部会发异步统计 MQ）
        orderService.markCompleted(orderId);
        log.info("配送员确认送达，订单完成：orderId={}", orderId);
    }

    @Override
    public DeliveryStatisticsVO statistics() {
        Long deliverymanId = BaseContext.getCurrentId();
        return DeliveryStatisticsVO.builder()
                .waiting(deliveryOrderMapper.countPoolWaiting())
                .accepted(deliveryOrderMapper.countByDeliverymanAndStatus(deliverymanId, DeliveryStatus.ACCEPTED.name()))
                .delivering(deliveryOrderMapper.countByDeliverymanAndStatus(deliverymanId, DeliveryStatus.DELIVERING.name()))
                .delivered(deliveryOrderMapper.countByDeliverymanAndStatus(deliverymanId, DeliveryStatus.DELIVERED.name()))
                .build();
    }

    /* ==================== 私有方法 ==================== */

    private DeliveryOrder requireMyDelivery(Long orderId, DeliveryStatus expected) {
        DeliveryOrder delivery = deliveryOrderMapper.getByOrderId(orderId);
        if (delivery == null) {
            throw new DeliveryBusinessException(MessageConstant.DELIVERY_ORDER_NOT_FOUND);
        }
        if (!BaseContext.getCurrentId().equals(delivery.getDeliverymanId())) {
            throw new DeliveryBusinessException(MessageConstant.DELIVERY_NO_PERMISSION);
        }
        if (!expected.name().equals(delivery.getStatus())) {
            throw new DeliveryBusinessException(MessageConstant.DELIVERY_STATUS_ERROR);
        }
        return delivery;
    }

    private DeliveryOrderVO buildVO(DeliveryOrder delivery) {
        DeliveryOrderVO vo = new DeliveryOrderVO();
        BeanUtils.copyProperties(delivery, vo);
        Orders orders = orderMapper.getById(delivery.getOrderId());
        if (orders != null) {
            vo.setOrderNumber(orders.getNumber());
            vo.setReceiverAddress(orders.getAddress());
            vo.setConsignee(orders.getConsignee());
            vo.setPhone(orders.getPhone());
            vo.setDeliveryFee(orders.getDeliveryFee());
        }
        Merchant merchant = merchantMapper.getById(delivery.getMerchantId());
        if (merchant != null) {
            vo.setShopName(merchant.getShopName());
            vo.setMerchantPhone(merchant.getPhone());
            vo.setPickupAddress(merchant.getAddress());
        }
        return vo;
    }
}
