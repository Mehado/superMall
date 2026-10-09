package com.supermall.takeout.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.takeout.config.RabbitMQConfig;
import com.supermall.constant.MessageConstant;
import com.supermall.takeout.constant.RedisKeyConstant;
import com.supermall.constant.StatusConstant;
import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.OrderPageQueryDTO;
import com.supermall.takeout.dto.OrderSubmitDTO;
import com.supermall.entity.Address;
import com.supermall.takeout.entity.CartItem;
import com.supermall.takeout.entity.DeliveryOrder;
import com.supermall.takeout.entity.Merchant;
import com.supermall.takeout.entity.OrderItem;
import com.supermall.takeout.entity.Orders;
import com.supermall.takeout.enumeration.DeliveryStatus;
import com.supermall.takeout.enumeration.OrderStatus;
import com.supermall.takeout.enumeration.PayStatus;
import com.supermall.takeout.exception.AddressBusinessException;
import com.supermall.takeout.exception.CartBusinessException;
import com.supermall.takeout.exception.OrderBusinessException;
import com.supermall.takeout.mapper.AddressMapper;
import com.supermall.takeout.mapper.CartItemMapper;
import com.supermall.takeout.mapper.DeliveryOrderMapper;
import com.supermall.takeout.mapper.MerchantMapper;
import com.supermall.takeout.mapper.OrderItemMapper;
import com.supermall.takeout.mapper.OrderMapper;
import com.supermall.takeout.mapper.ProductMapper;
import com.supermall.takeout.mapper.SkuMapper;
import com.supermall.result.PageResult;
import com.supermall.takeout.service.MerchantService;
import com.supermall.takeout.service.OrderService;
import com.supermall.takeout.service.StockService;
import com.supermall.takeout.vo.OrderStatisticsVO;
import com.supermall.takeout.vo.OrderSubmitVO;
import com.supermall.takeout.vo.OrderVO;
import com.supermall.takeout.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单服务实现：串联「购物车 -> 按商家拆单 -> 扣库存 -> 支付 -> 商家接单 -> 备餐 -> 配送 -> 完成」全流程。
 *
 * 关键设计：
 * 1. 幂等：Redis 令牌（Lua 原子消费）+ 数据库唯一索引（user_id, submit_token）双重兜底
 * 2. 防超卖：Redis + Lua 预扣，DB 条件更新（stock >= number）二次保证
 * 3. 状态流转：全部使用「条件更新」（where status = 期望值），天然防并发重复流转
 * 4. 超时关单：MQ 延迟队列 + 定时任务兜底，均走同一条件更新方法
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final AddressMapper addressMapper;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final MerchantMapper merchantMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final StockService stockService;
    private final MerchantService merchantService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final WebSocketServer webSocketServer;
    /** 幂等令牌消费脚本：由 RedisScriptConfig 从 .lua 文件加载为 Bean */
    private final DefaultRedisScript<Long> idempotentTokenScript;

    @Value("${super-mall.delivery.base-fee:3.00}")
    private BigDecimal baseDeliveryFee;

    /* ==================== 用户端 ==================== */

    @Override
    public String getSubmitToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstant.SUBMIT_TOKEN_PREFIX + token,
                String.valueOf(BaseContext.getCurrentId()),
                10, TimeUnit.MINUTES);
        return token;
    }

    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrderSubmitDTO dto) {
        Long userId = BaseContext.getCurrentId();

        // 1. Redis 幂等令牌：检查 + 删除原子执行
        Long consumed = stringRedisTemplate.execute(
                idempotentTokenScript,
                Collections.singletonList(RedisKeyConstant.SUBMIT_TOKEN_PREFIX + dto.getToken()),
                String.valueOf(userId));
        if (consumed == null || consumed != 1L) {
            throw new OrderBusinessException(MessageConstant.ORDER_REPEAT_SUBMIT);
        }

        // 2. 收货地址校验（归属）
        Address address = addressMapper.getById(dto.getAddressId());
        if (address == null || !userId.equals(address.getUserId())) {
            throw new AddressBusinessException(MessageConstant.ADDRESS_IS_NULL);
        }

        // 3. 该商家购物车明细
        List<CartItem> cartItems = cartItemMapper.listByUserIdAndMerchant(userId, dto.getMerchantId());
        if (cartItems == null || cartItems.isEmpty()) {
            throw new CartBusinessException(MessageConstant.CART_IS_NULL);
        }

        // 3.5 商家自助营业状态校验（Redis 缓存 + DB 回源）：休息中不接单
        if (StatusConstant.DISABLE.equals(merchantService.getBusinessStatus(dto.getMerchantId()))) {
            throw new OrderBusinessException(MessageConstant.MERCHANT_CLOSED);
        }

        // 4. 数据库层幂等兜底
        if (orderMapper.countByUserAndToken(userId, dto.getToken()) > 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_REPEAT_SUBMIT);
        }

        // 5. 服务端重新计算金额，不信任前端
        BigDecimal amount = cartItems.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getNumber())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deliveryFee = baseDeliveryFee == null ? BigDecimal.ZERO : baseDeliveryFee;

        // 6. Redis + Lua 预扣库存
        stockService.deductStock(cartItems);

        List<OrderItem> orderItems = buildOrderItems(cartItems, dto.getMerchantId());
        try {
            // 7. DB 条件扣库存（stock >= number），二次防超卖
            deductDbStock(orderItems);

            // 8. 落订单 + 明细
            LocalDateTime now = LocalDateTime.now();
            Orders orders = Orders.builder()
                    .number(generateOrderNumber())
                    .submitToken(dto.getToken())
                    .userId(userId)
                    .merchantId(dto.getMerchantId())
                    .addressId(address.getId())
                    .status(OrderStatus.PENDING_PAYMENT.name())
                    .payStatus(PayStatus.UN_PAID.name())
                    .payMethod(dto.getPayMethod())
                    .amount(amount)
                    .deliveryFee(deliveryFee)
                    .remark(dto.getRemark())
                    .consignee(address.getConsignee())
                    .phone(address.getPhone())
                    .address(fullAddress(address))
                    .orderTime(now)
                    .estimatedDeliveryTime(dto.getEstimatedDeliveryTime())
                    .createTime(now)
                    .updateTime(now)
                    .build();
            orderMapper.insert(orders);

            for (OrderItem item : orderItems) {
                item.setOrderId(orders.getId());
            }
            orderItemMapper.batchInsert(orderItems);

            // 9. 清空该商家购物车
            cartItemMapper.deleteByCartId(cartItems.get(0).getCartId());

            // 10. 发送延迟关单消息（失败由定时任务兜底）
            sendDelayCloseMessage(orders.getId());

            return OrderSubmitVO.builder()
                    .id(orders.getId())
                    .orderNumber(orders.getNumber())
                    .orderAmount(orders.getAmount().add(orders.getDeliveryFee()))
                    .orderTime(orders.getOrderTime())
                    .build();
        } catch (DuplicateKeyException e) {
            log.warn("数据库唯一索引拦截重复下单：userId={}, token={}", userId, dto.getToken());
            stockService.releaseStock(orderItems);
            throw new OrderBusinessException(MessageConstant.ORDER_REPEAT_SUBMIT);
        } catch (Exception e) {
            // 事务回滚 DB，Redis 库存手动回补
            log.error("下单失败，回补 Redis 库存", e);
            stockService.releaseStock(orderItems);
            throw e;
        }
    }

    @Override
    public PageResult pageQuery4User(int page, int pageSize, String status) {
        PageHelper.startPage(page, pageSize);
        OrderPageQueryDTO dto = new OrderPageQueryDTO();
        dto.setUserId(BaseContext.getCurrentId());
        dto.setStatus(status);
        Page<Orders> p = orderMapper.pageQuery(dto);
        return new PageResult(p.getTotal(), buildOrderVOList(p.getResult()));
    }

    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null || !BaseContext.getCurrentId().equals(orders.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return buildOrderVO(orders);
    }

    @Override
    @Transactional
    public void userCancel(Long id) {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getById(id);
        if (orders == null || !userId.equals(orders.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!OrderStatus.isCancellable(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders update = Orders.builder()
                .payStatus(PayStatus.PAID.name().equals(orders.getPayStatus())
                        ? PayStatus.REFUND.name() : null)
                .cancelReason(MessageConstant.ORDER_CANCEL_BY_USER)
                .cancelTime(LocalDateTime.now())
                .build();
        int rows = orderMapper.transitionStatus(id, orders.getStatus(), OrderStatus.CANCELLED.name(), update);
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        releaseStock(id);
    }

    /* ==================== 商家端 ==================== */

    @Override
    public PageResult pageQuery4Merchant(OrderPageQueryDTO dto) {
        dto.setMerchantId(BaseContext.getCurrentId());
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<Orders> p = orderMapper.pageQuery(dto);
        return new PageResult(p.getTotal(), buildOrderVOList(p.getResult()));
    }

    @Override
    public OrderVO merchantDetails(Long id) {
        Orders orders = requireMerchantOrder(id);
        return buildOrderVO(orders);
    }

    @Override
    public void merchantAccept(Long id) {
        Orders orders = requireMerchantOrder(id);
        int rows = orderMapper.transitionStatus(id, OrderStatus.PAID.name(),
                OrderStatus.MERCHANT_ACCEPTED.name(), new Orders());
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        notifyUser(orders.getUserId(), "商家已接单", orders.getId());
    }

    @Override
    @Transactional
    public void merchantReady(Long id) {
        Orders orders = requireMerchantOrder(id);
        int rows = orderMapper.transitionStatus(id, OrderStatus.MERCHANT_ACCEPTED.name(),
                OrderStatus.WAITING_DELIVERY.name(), new Orders());
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 幂等：已存在配送单则不重复创建
        if (deliveryOrderMapper.getByOrderId(id) == null) {
            LocalDateTime now = LocalDateTime.now();
            deliveryOrderMapper.insert(DeliveryOrder.builder()
                    .orderId(id)
                    .merchantId(orders.getMerchantId())
                    .status(DeliveryStatus.WAITING.name())
                    .createTime(now)
                    .updateTime(now)
                    .build());
        }
        notifyUser(orders.getUserId(), "商家已备餐完成，等待配送", orders.getId());
    }

    @Override
    @Transactional
    public void merchantReject(Long id, String reason) {
        Orders orders = requireMerchantOrder(id);
        Orders update = Orders.builder()
                .payStatus(PayStatus.PAID.name().equals(orders.getPayStatus())
                        ? PayStatus.REFUND.name() : null)
                .cancelReason(reason)
                .cancelTime(LocalDateTime.now())
                .build();
        int rows = orderMapper.transitionStatus(id, OrderStatus.PAID.name(),
                OrderStatus.CANCELLED.name(), update);
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        releaseStock(id);
        notifyUser(orders.getUserId(), "商家拒单：" + reason, orders.getId());
    }

    @Override
    public OrderStatisticsVO statistics() {
        Long merchantId = BaseContext.getCurrentId();
        // 今日区间 [今天 00:00, 明天 00:00)
        LocalDateTime dayBegin = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayBegin.plusDays(1);
        Double todayAmount = orderMapper.sumAmountByMerchant(merchantId, dayBegin, dayEnd);
        return OrderStatisticsVO.builder()
                .pendingPayment(orderMapper.countByStatus(merchantId, OrderStatus.PENDING_PAYMENT.name()))
                .paid(orderMapper.countByStatus(merchantId, OrderStatus.PAID.name()))
                .merchantAccepted(orderMapper.countByStatus(merchantId, OrderStatus.MERCHANT_ACCEPTED.name()))
                .waitingDelivery(orderMapper.countByStatus(merchantId, OrderStatus.WAITING_DELIVERY.name()))
                .delivering(orderMapper.countByStatus(merchantId, OrderStatus.DELIVERING.name()))
                .completed(orderMapper.countByStatus(merchantId, OrderStatus.COMPLETED.name()))
                .todayOrders(orderMapper.countByMerchantAndTime(merchantId, dayBegin, dayEnd))
                .todayAmount(todayAmount == null ? 0.0 : todayAmount)
                .build();
    }

    /* ==================== 支付 ==================== */

    @Override
    public void paySuccess(String orderNumber, String payMethod) {
        int rows = orderMapper.markPaidIfPending(orderNumber, LocalDateTime.now(), payMethod);
        if (rows == 0) {
            log.info("支付回调幂等跳过：订单 {} 不是待支付状态", orderNumber);
            return;
        }
        Orders orders = orderMapper.getByNumber(orderNumber);
        // 支付成功异步处理
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAY_EXCHANGE, RabbitMQConfig.PAY_ROUTING_KEY, orders.getId());
        notifyUser(orders.getUserId(), "支付成功", orders.getId());
        log.info("支付成功：orderNumber={}, orderId={}", orderNumber, orders.getId());
    }

    /* ==================== 配送联动 ==================== */

    @Override
    public void markDelivering(Long orderId) {
        int rows = orderMapper.transitionStatus(orderId, OrderStatus.WAITING_DELIVERY.name(),
                OrderStatus.DELIVERING.name(), new Orders());
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    @Override
    public void markCompleted(Long orderId) {
        Orders update = Orders.builder().deliveryTime(LocalDateTime.now()).build();
        int rows = orderMapper.transitionStatus(orderId, OrderStatus.DELIVERING.name(),
                OrderStatus.COMPLETED.name(), update);
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 订单完成 -> 异步统计（更新销量）
        rabbitTemplate.convertAndSend(RabbitMQConfig.STAT_EXCHANGE, RabbitMQConfig.STAT_ROUTING_KEY, orderId);
    }

    /* ==================== 超时兜底 ==================== */

    @Override
    public void cancelTimeoutOrder(Long orderId) {
        int rows = orderMapper.cancelByIdIfPendingPayment(orderId, LocalDateTime.now(),
                MessageConstant.ORDER_TIMEOUT_CANCEL);
        if (rows > 0) {
            releaseStock(orderId);
            log.info("超时关单成功并回补库存：orderId={}", orderId);
        } else {
            log.info("超时关单跳过（状态已变更）：orderId={}", orderId);
        }
    }

    /* ==================== 私有方法 ==================== */

    private List<OrderItem> buildOrderItems(List<CartItem> cartItems, Long merchantId) {
        List<OrderItem> list = new ArrayList<>();
        for (CartItem c : cartItems) {
            list.add(OrderItem.builder()
                    .merchantId(merchantId)
                    .productId(c.getProductId())
                    .skuId(c.getSkuId())
                    .productName(c.getProductName())
                    .skuName(c.getSkuName())
                    .image(c.getImage())
                    .price(c.getPrice())
                    .number(c.getNumber())
                    .amount(c.getPrice().multiply(BigDecimal.valueOf(c.getNumber())))
                    .build());
        }
        return list;
    }

    /** DB 条件扣库存：任一失败即抛异常，事务回滚前面的扣减 */
    private void deductDbStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            int rows;
            if (item.getSkuId() != null && item.getSkuId() > 0) {
                rows = skuMapper.deductStock(item.getSkuId(), item.getNumber());
            } else {
                rows = productMapper.deductStock(item.getProductId(), item.getNumber());
            }
            if (rows == 0) {
                throw new OrderBusinessException(MessageConstant.STOCK_NOT_ENOUGH);
            }
        }
    }

    /** 回补库存：Redis + DB */
    private void releaseStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.listByOrderId(orderId);
        stockService.releaseStock(items);
        for (OrderItem item : items) {
            if (item.getSkuId() != null && item.getSkuId() > 0) {
                skuMapper.releaseStock(item.getSkuId(), item.getNumber());
            } else {
                productMapper.releaseStock(item.getProductId(), item.getNumber());
            }
        }
    }

    private Orders requireMerchantOrder(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null || !BaseContext.getCurrentId().equals(orders.getMerchantId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NO_PERMISSION);
        }
        return orders;
    }

    private List<OrderVO> buildOrderVOList(List<Orders> ordersList) {
        if (ordersList == null || ordersList.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> orderIds = ordersList.stream().map(Orders::getId).collect(Collectors.toList());
        List<OrderItem> allItems = orderItemMapper.listByOrderIds(orderIds);
        Map<Long, List<OrderItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return ordersList.stream()
                .map(o -> buildOrderVO(o, itemMap.getOrDefault(o.getId(), new ArrayList<>())))
                .collect(Collectors.toList());
    }

    private OrderVO buildOrderVO(Orders orders) {
        return buildOrderVO(orders, orderItemMapper.listByOrderId(orders.getId()));
    }

    private OrderVO buildOrderVO(Orders orders, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(orders, vo);
        vo.setOrderItems(items);
        Merchant merchant = merchantMapper.getById(orders.getMerchantId());
        if (merchant != null) {
            vo.setShopName(merchant.getShopName());
        }
        DeliveryOrder delivery = deliveryOrderMapper.getByOrderId(orders.getId());
        if (delivery != null) {
            vo.setDeliveryStatus(delivery.getStatus());
        }
        return vo;
    }

    private void sendDelayCloseMessage(Long orderId) {
        try {
            CorrelationData cd = new CorrelationData("order-close-" + orderId + "-" + UUID.randomUUID());
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_DELAY_ROUTING_KEY, orderId, cd);
        } catch (Exception e) {
            log.error("延迟关单消息发送失败，等待定时任务兜底：orderId={}", orderId, e);
        }
    }

    private void notifyUser(Long userId, String content, Long orderId) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("type", "ORDER_STATUS");
            map.put("orderId", orderId);
            map.put("content", content);
            webSocketServer.sendToUser(userId, JSON.toJSONString(map));
        } catch (Exception e) {
            log.warn("WebSocket 推送失败：userId={}", userId, e);
        }
    }

    private String generateOrderNumber() {
        return System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }

    private String fullAddress(Address a) {
        StringBuilder sb = new StringBuilder();
        if (a.getProvince() != null) sb.append(a.getProvince());
        if (a.getCity() != null) sb.append(a.getCity());
        if (a.getDistrict() != null) sb.append(a.getDistrict());
        sb.append(a.getDetail());
        return sb.toString();
    }
}
