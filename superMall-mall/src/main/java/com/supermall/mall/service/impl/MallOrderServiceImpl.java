package com.supermall.mall.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.context.BaseContext;
import com.supermall.entity.Address;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.constant.MallRedisKeyConstant;
import com.supermall.mall.dto.MallOrderPageQueryDTO;
import com.supermall.mall.dto.MallOrderSubmitDTO;
import com.supermall.mall.dto.MallShipDTO;
import com.supermall.mall.entity.MallOrder;
import com.supermall.mall.entity.MallOrderItem;
import com.supermall.mall.entity.MallRefund;
import com.supermall.mall.entity.MallShipment;
import com.supermall.mall.enumeration.MallOrderStatus;
import com.supermall.mall.enumeration.MallPayStatus;
import com.supermall.mall.enumeration.MallRefundStatus;
import com.supermall.mall.enumeration.MallRefundType;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallAddressMapper;
import com.supermall.mall.mapper.MallCartItemMapper;
import com.supermall.mall.mapper.MallOrderItemMapper;
import com.supermall.mall.mapper.MallOrderMapper;
import com.supermall.mall.mapper.MallRefundMapper;
import com.supermall.mall.mapper.MallShipmentMapper;
import com.supermall.mall.service.MallOrderService;
import com.supermall.mall.service.MallStockService;
import com.supermall.mall.utils.MallCartValidators;
import com.supermall.mall.vo.MallOrderItemVO;
import com.supermall.mall.vo.MallOrderSubmitVO;
import com.supermall.mall.vo.MallOrderVO;
import com.supermall.mall.vo.MallRefundVO;
import com.supermall.mall.vo.MallShipmentVO;
import com.supermall.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单服务实现。
 *
 * <p>下单链路（与外卖同思路，但电商一定有 SKU、且不拆单）：
 * <pre>
 * 幂等令牌(Lua) → 校验地址 → 取购物车已选行 → 校验失效/库存
 *   → 服务端算金额 → Redis 预扣 → DB 条件扣减(stock>=n) → 重算 SPU 聚合
 *   → 落订单+明细(快照) → 删已结算购物车行
 * 任一步失败：DB 事务回滚 + Redis 手动回补
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallOrderServiceImpl implements MallOrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final int TOKEN_TTL_MINUTES = 10;

    private final MallOrderMapper mallOrderMapper;
    private final MallOrderItemMapper mallOrderItemMapper;
    private final MallShipmentMapper mallShipmentMapper;
    private final MallRefundMapper mallRefundMapper;
    private final MallCartItemMapper mallCartItemMapper;
    private final MallAddressMapper mallAddressMapper;
    private final MallStockService mallStockService;
    private final StringRedisTemplate stringRedisTemplate;
    /** 幂等令牌消费脚本（MallRedisScriptConfig 加载） */
    private final DefaultRedisScript<Long> mallIdempotentTokenScript;

    @Value("${super-mall.order.pay-timeout-minutes:15}")
    private long payTimeoutMinutes;

    @Value("${super-mall.order.freight-amount:0.00}")
    private BigDecimal freightAmount;

    // ============================================================
    // 买家：下单
    // ============================================================

    @Override
    public String getSubmitToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                MallRedisKeyConstant.SUBMIT_TOKEN_PREFIX + token,
                String.valueOf(BaseContext.getCurrentId()),
                TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    @Override
    @Transactional
    public MallOrderSubmitVO submit(MallOrderSubmitDTO dto) {
        Long userId = BaseContext.getCurrentId();

        // 1) Redis 幂等令牌：检查 + 删除原子执行
        Long consumed = stringRedisTemplate.execute(
                mallIdempotentTokenScript,
                Collections.singletonList(MallRedisKeyConstant.SUBMIT_TOKEN_PREFIX + dto.getSubmitToken()),
                String.valueOf(userId));
        if (consumed == null || consumed != 1L) {
            throw new MallBusinessException(MallMessageConstant.ORDER_REPEAT_SUBMIT);
        }

        // 2) 收货地址（快照进订单）
        Address address = mallAddressMapper.getById(dto.getAddressId());
        if (address == null || !userId.equals(address.getUserId())) {
            throw new MallBusinessException(MallMessageConstant.ADDRESS_NOT_FOUND);
        }

        // 3) 购物车里「已选中」的行
        List<com.supermall.mall.vo.MallCartItemVO> cartItems = mallCartItemMapper.listSelectedByUser(userId);
        if (cartItems.isEmpty()) {
            throw new MallBusinessException(MallMessageConstant.ORDER_CART_EMPTY);
        }

        // 4) 逐行校验有效性（失效就明确报出是哪件商品，而不是静默少结算）
        for (com.supermall.mall.vo.MallCartItemVO ci : cartItems) {
            String reason = MallCartValidators.invalidReason(ci);
            if (reason != null) {
                throw new MallBusinessException(
                        MallMessageConstant.ORDER_ITEM_INVALID_PREFIX + ci.getProductName() + "」" + reason);
            }
        }

        // 5) DB 唯一键幂等兜底
        if (mallOrderMapper.countByUserAndToken(userId, dto.getSubmitToken()) > 0) {
            throw new MallBusinessException(MallMessageConstant.ORDER_REPEAT_SUBMIT);
        }

        // 6) 服务端算金额，不信任前端
        BigDecimal totalAmount = cartItems.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getNumber())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = BigDecimal.ZERO;   // 优惠券是 P6
        BigDecimal freight = freightAmount == null ? BigDecimal.ZERO : freightAmount;
        BigDecimal payAmount = totalAmount.subtract(discount).add(freight);

        String orderNumber = generateOrderNumber();
        List<MallOrderItem> orderItems = buildOrderItems(cartItems, orderNumber);

        // 7) Redis 预扣（挡掉绝大部分并发）
        mallStockService.deductRedis(orderItems);

        LocalDateTime now = LocalDateTime.now();
        try {
            // 8) DB 条件扣减（最终一致）+ 重算 SPU 冗余字段
            mallStockService.deductDb(orderItems);
            mallStockService.refreshAggregates(orderItems);

            // 9) 落订单 + 明细快照
            MallOrder order = MallOrder.builder()
                    .orderNumber(orderNumber)
                    .userId(userId)
                    .status(MallOrderStatus.PENDING_PAYMENT.name())
                    .payStatus(MallPayStatus.UN_PAID.name())
                    .totalAmount(totalAmount)
                    .discountAmount(discount)
                    .freightAmount(freight)
                    .payAmount(payAmount)
                    .receiverName(address.getConsignee())
                    .receiverPhone(address.getPhone())
                    .receiverProvince(address.getProvince())
                    .receiverCity(address.getCity())
                    .receiverDistrict(address.getDistrict())
                    .receiverAreaCode(address.getAreaCode())
                    .receiverDetail(address.getDetail())
                    .remark(dto.getRemark())
                    .submitToken(dto.getSubmitToken())
                    .expireTime(now.plusMinutes(payTimeoutMinutes))
                    .createTime(now)
                    .updateTime(now)
                    .build();
            mallOrderMapper.insert(order);

            for (MallOrderItem item : orderItems) {
                item.setOrderId(order.getId());
            }
            mallOrderItemMapper.batchInsert(orderItems);

            // 10) 删掉已结算的购物车行
            mallCartItemMapper.deleteByIds(userId, cartItems.stream()
                    .map(com.supermall.mall.vo.MallCartItemVO::getId).toList());

            log.info("下单成功：orderNumber={}, userId={}, 商品={} 件, 实付={}",
                    orderNumber, userId, orderItems.size(), payAmount);

            return MallOrderSubmitVO.builder()
                    .id(order.getId())
                    .orderNumber(orderNumber)
                    .totalAmount(totalAmount)
                    .discountAmount(discount)
                    .freightAmount(freight)
                    .payAmount(payAmount)
                    .createTime(now)
                    .expireTime(order.getExpireTime())
                    .build();
        } catch (DuplicateKeyException e) {
            log.warn("唯一索引拦截重复下单：userId={}, token={}", userId, dto.getSubmitToken());
            mallStockService.releaseRedis(orderItems);
            throw new MallBusinessException(MallMessageConstant.ORDER_REPEAT_SUBMIT);
        } catch (Exception e) {
            // DB 随事务回滚，Redis 必须手动回补
            mallStockService.releaseRedis(orderItems);
            throw e;
        }
    }

    // ============================================================
    // 买家：查询 / 取消 / 收货
    // ============================================================

    @Override
    public PageResult pageForUser(String status, Integer page, Integer pageSize) {
        MallOrderPageQueryDTO query = new MallOrderPageQueryDTO();
        query.setUserId(BaseContext.getCurrentId());
        query.setStatus(status);
        query.setPage(page);
        query.setPageSize(pageSize);
        PageHelper.startPage(query.safePage(), query.safePageSize());
        Page<MallOrder> p = mallOrderMapper.page(query);
        return new PageResult(p.getTotal(), buildVoList(p.getResult()));
    }

    @Override
    public MallOrderVO detailForUser(String orderNumber) {
        return toVO(requireOwned(BaseContext.getCurrentId(), orderNumber));
    }

    @Override
    @Transactional
    public void cancel(String orderNumber) {
        MallOrder order = requireOwned(BaseContext.getCurrentId(), orderNumber);
        cancelInternal(order, MallMessageConstant.ORDER_CANCEL_BY_USER);
    }

    @Override
    @Transactional
    public void confirmReceipt(String orderNumber) {
        MallOrder order = requireOwned(BaseContext.getCurrentId(), orderNumber);
        int rows = mallOrderMapper.transitionStatus(order.getId(),
                MallOrderStatus.PENDING_RECEIPT.name(),
                MallOrderStatus.COMPLETED.name(),
                MallOrder.builder().receiveTime(LocalDateTime.now()).build());
        if (rows == 0) {
            throw new MallBusinessException(MallMessageConstant.ORDER_STATUS_ERROR);
        }
        log.info("确认收货：orderNumber={}", orderNumber);
    }

    // ============================================================
    // 支付成功 / 超时取消
    // ============================================================

    @Override
    @Transactional
    public boolean paySuccess(String orderNumber, String payMethod) {
        int rows = mallOrderMapper.markPaidIfPending(orderNumber, LocalDateTime.now(), payMethod);
        if (rows == 0) {
            log.info("支付幂等跳过（订单已不是待付款）：orderNumber={}", orderNumber);
            return false;
        }
        MallOrder order = mallOrderMapper.getByNumber(orderNumber);
        // 支付成功才计入销量（未付款的订单不算销量）
        mallStockService.adjustSales(mallOrderItemMapper.listByOrderId(order.getId()), 1);
        log.info("支付成功：orderNumber={}, orderId={}, method={}", orderNumber, order.getId(), payMethod);
        return true;
    }

    @Override
    @Transactional
    public void cancelTimeoutOrder(Long orderId) {
        MallOrder order = mallOrderMapper.getById(orderId);
        if (order == null) {
            return;
        }
        int rows = mallOrderMapper.cancelIfPendingPayment(order.getId(),
                LocalDateTime.now(), MallMessageConstant.ORDER_TIMEOUT_CANCEL);
        if (rows == 0) {
            log.info("超时关单跳过（状态已变更）：orderId={}", orderId);
            return;
        }
        List<MallOrderItem> items = mallOrderItemMapper.listByOrderId(orderId);
        mallStockService.releaseDb(items);
        mallStockService.refreshAggregates(items);
        mallStockService.releaseRedis(items);
        log.info("超时关单成功并回补库存：orderId={}, orderNumber={}", orderId, order.getOrderNumber());
    }

    // ============================================================
    // 运营后台
    // ============================================================

    @Override
    public PageResult pageForAdmin(MallOrderPageQueryDTO query) {
        PageHelper.startPage(query.safePage(), query.safePageSize());
        Page<MallOrder> p = mallOrderMapper.page(query);
        return new PageResult(p.getTotal(), buildVoList(p.getResult()));
    }

    @Override
    public MallOrderVO detailForAdmin(String orderNumber) {
        return toVO(requireByNumber(orderNumber));
    }

    @Override
    @Transactional
    public void ship(String orderNumber, MallShipDTO dto) {
        MallOrder order = requireByNumber(orderNumber);
        if (mallShipmentMapper.getByOrderId(order.getId()) != null) {
            throw new MallBusinessException(MallMessageConstant.ORDER_ALREADY_SHIPPED);
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = mallOrderMapper.transitionStatus(order.getId(),
                MallOrderStatus.PENDING_DELIVERY.name(),
                MallOrderStatus.PENDING_RECEIPT.name(),
                MallOrder.builder().deliveryTime(now).build());
        if (rows == 0) {
            throw new MallBusinessException(MallMessageConstant.ORDER_STATUS_ERROR);
        }
        mallShipmentMapper.insert(MallShipment.builder()
                .orderId(order.getId())
                .orderNumber(orderNumber)
                .company(dto.getCompany().trim())
                .trackingNo(dto.getTrackingNo().trim())
                .shipTime(now)
                .createTime(now)
                .updateTime(now)
                .build());
        log.info("发货成功：orderNumber={}, company={}, trackingNo={}",
                orderNumber, dto.getCompany(), dto.getTrackingNo());
    }

    // ============================================================
    // 校验 / 组装
    // ============================================================

    @Override
    public MallOrder requireOwned(Long userId, String orderNumber) {
        MallOrder order = requireByNumber(orderNumber);
        if (!userId.equals(order.getUserId())) {
            throw new MallBusinessException(MallMessageConstant.ORDER_NOT_FOUND);
        }
        return order;
    }

    private MallOrder requireByNumber(String orderNumber) {
        MallOrder order = orderNumber == null ? null : mallOrderMapper.getByNumber(orderNumber);
        if (order == null) {
            throw new MallBusinessException(MallMessageConstant.ORDER_NOT_FOUND);
        }
        return order;
    }

    @Override
    public MallOrderVO toVO(MallOrder order) {
        MallOrderVO vo = new MallOrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setStatusDesc(MallOrderStatus.descOf(order.getStatus()));
        vo.setItems(toItemVOs(mallOrderItemMapper.listByOrderId(order.getId())));

        MallShipment shipment = mallShipmentMapper.getByOrderId(order.getId());
        if (shipment != null) {
            vo.setShipment(MallShipmentVO.builder()
                    .company(shipment.getCompany())
                    .trackingNo(shipment.getTrackingNo())
                    .shipTime(shipment.getShipTime())
                    .build());
        }
        MallRefund refund = mallRefundMapper.getByOrderId(order.getId());
        if (refund != null) {
            vo.setRefund(toRefundBrief(refund));
        }
        return vo;
    }

    private List<MallOrderVO> buildVoList(List<MallOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return new ArrayList<>();
        }
        // 明细一次查完，避免 N+1
        List<Long> orderIds = orders.stream().map(MallOrder::getId).toList();
        Map<Long, List<MallOrderItem>> itemMap = new LinkedHashMap<>();
        for (MallOrderItem item : mallOrderItemMapper.listByOrderIds(orderIds)) {
            itemMap.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
        }
        // 物流单一次查完
        Map<Long, MallShipment> shipmentMap = mallShipmentMapper.listByOrderIds(orderIds).stream()
                .collect(Collectors.toMap(MallShipment::getOrderId, s -> s, (a, b) -> a));
        // 退款单一次查完（同一单可能有多条历史申请，id 降序，保留最新一条）
        Map<Long, MallRefund> refundMap = new LinkedHashMap<>();
        for (MallRefund r : mallRefundMapper.listByOrderIds(orderIds)) {
            refundMap.putIfAbsent(r.getOrderId(), r);
        }
        List<MallOrderVO> result = new ArrayList<>();
        for (MallOrder order : orders) {
            MallOrderVO vo = new MallOrderVO();
            BeanUtils.copyProperties(order, vo);
            vo.setStatusDesc(MallOrderStatus.descOf(order.getStatus()));
            vo.setItems(toItemVOs(itemMap.getOrDefault(order.getId(), List.of())));
            MallShipment shipment = shipmentMap.get(order.getId());
            if (shipment != null) {
                vo.setShipment(MallShipmentVO.builder()
                        .company(shipment.getCompany())
                        .trackingNo(shipment.getTrackingNo())
                        .shipTime(shipment.getShipTime())
                        .build());
            }
            MallRefund refund = refundMap.get(order.getId());
            if (refund != null) {
                vo.setRefund(toRefundBrief(refund));
            }
            result.add(vo);
        }
        return result;
    }

    private List<MallOrderItemVO> toItemVOs(List<MallOrderItem> items) {
        List<MallOrderItemVO> list = new ArrayList<>();
        for (MallOrderItem i : items) {
            list.add(MallOrderItemVO.builder()
                    .id(i.getId())
                    .productId(i.getProductId())
                    .skuId(i.getSkuId())
                    .productName(i.getProductName())
                    .skuCode(i.getSkuCode())
                    .image(i.getImage())
                    .specText(i.getSpecText())
                    .price(i.getPrice())
                    .number(i.getNumber())
                    .totalAmount(i.getTotalAmount())
                    .build());
        }
        return list;
    }

    private MallRefundVO toRefundBrief(MallRefund r) {
        return MallRefundVO.builder()
                .id(r.getId())
                .refundNumber(r.getRefundNumber())
                .orderId(r.getOrderId())
                .orderNumber(r.getOrderNumber())
                .userId(r.getUserId())
                .type(r.getType())
                .typeDesc(MallRefundType.descOf(r.getType()))
                .reason(r.getReason())
                .description(r.getDescription())
                .amount(r.getAmount())
                .status(r.getStatus())
                .statusDesc(MallRefundStatus.descOf(r.getStatus()))
                .previousStatus(r.getPreviousStatus())
                .adminRemark(r.getAdminRemark())
                .auditTime(r.getAuditTime())
                .createTime(r.getCreateTime())
                .updateTime(r.getUpdateTime())
                .build();
    }

    /** 取消：条件更新 + 回补库存（幂等，只有真正取消的那次才回补） */
    private void cancelInternal(MallOrder order, String reason) {
        int rows = mallOrderMapper.cancelIfPendingPayment(order.getId(), LocalDateTime.now(), reason);
        if (rows == 0) {
            throw new MallBusinessException(MallMessageConstant.ORDER_STATUS_ERROR);
        }
        List<MallOrderItem> items = mallOrderItemMapper.listByOrderId(order.getId());
        mallStockService.releaseDb(items);
        mallStockService.refreshAggregates(items);
        mallStockService.releaseRedis(items);
        log.info("取消订单并回补库存：orderNumber={}, reason={}", order.getOrderNumber(), reason);
    }

    private List<MallOrderItem> buildOrderItems(List<com.supermall.mall.vo.MallCartItemVO> cartItems,
                                                String orderNumber) {
        List<MallOrderItem> list = new ArrayList<>();
        for (com.supermall.mall.vo.MallCartItemVO c : cartItems) {
            list.add(MallOrderItem.builder()
                    .orderNumber(orderNumber)
                    .productId(c.getProductId())
                    .skuId(c.getSkuId())
                    .productName(c.getProductName())
                    .skuCode(c.getSkuCode())
                    .image(c.getImage())
                    .specText(c.getSpecText())
                    .price(c.getPrice())
                    .number(c.getNumber())
                    .totalAmount(c.getPrice().multiply(BigDecimal.valueOf(c.getNumber())))
                    .build());
        }
        return list;
    }

    /**
     * 订单号规则：{@code yyyyMMddHHmmssSSS} + 3 位随机数（共 20 位）。
     * 毫秒级时间戳 + 随机后缀，单机碰撞概率极低；真撞了有唯一索引兜底。
     */
    private String generateOrderNumber() {
        return LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}
