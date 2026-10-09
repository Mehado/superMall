package com.supermall.mall.service.impl;

import com.supermall.context.BaseContext;
import com.supermall.entity.Address;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallOrderSubmitDTO;
import com.supermall.mall.dto.MallShipDTO;
import com.supermall.mall.entity.MallOrder;
import com.supermall.mall.entity.MallOrderItem;
import com.supermall.mall.entity.MallShipment;
import com.supermall.mall.enumeration.MallOrderStatus;
import com.supermall.mall.enumeration.MallPayStatus;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallAddressMapper;
import com.supermall.mall.mapper.MallCartItemMapper;
import com.supermall.mall.mapper.MallOrderItemMapper;
import com.supermall.mall.mapper.MallOrderMapper;
import com.supermall.mall.mapper.MallRefundMapper;
import com.supermall.mall.mapper.MallShipmentMapper;
import com.supermall.mall.service.MallStockService;
import com.supermall.mall.vo.MallCartItemVO;
import com.supermall.mall.vo.MallOrderSubmitVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单核心逻辑：下单金额/幂等/失效拦截、取消回补、支付、发货。
 *
 * <p>不依赖中间件（Redis / Mapper 全部 mock）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallOrderServiceImpl - 订单")
class MallOrderServiceImplTest {

    private static final Long USER = 7L;
    private static final String ORDER_NO = "20260920120000001123";

    @Mock private MallOrderMapper orderMapper;
    @Mock private MallOrderItemMapper orderItemMapper;
    @Mock private MallShipmentMapper shipmentMapper;
    @Mock private MallRefundMapper refundMapper;
    @Mock private MallCartItemMapper cartMapper;
    @Mock private MallAddressMapper addressMapper;
    @Mock private MallStockService stockService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private DefaultRedisScript<Long> tokenScript;

    private MallOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(USER);
        service = new MallOrderServiceImpl(orderMapper, orderItemMapper, shipmentMapper, refundMapper,
                cartMapper, addressMapper, stockService, redisTemplate, tokenScript);
        ReflectionTestUtils.setField(service, "payTimeoutMinutes", 15L);
        ReflectionTestUtils.setField(service, "freightAmount", new BigDecimal("0.00"));
        lenient().when(redisTemplate.execute(any(), anyList(), any())).thenReturn(1L);
        lenient().when(addressMapper.getById(anyLong())).thenReturn(Address.builder()
                .id(1L).userId(USER).consignee("张三").phone("13800138000")
                .province("北京市").city("北京市").district("东城区").detail("某路 1 号").build());
        lenient().doAnswer(inv -> {
            inv.getArgument(0, MallOrder.class).setId(99L);
            return null;
        }).when(orderMapper).insert(any(MallOrder.class));
    }

    @AfterEach
    void tearDown() {
        BaseContext.clear();
    }

    private MallCartItemVO cartItem(int number) {        return MallCartItemVO.builder()
                .id(5L).productId(1L).skuId(1L).number(number).selected(1)
                .productName("纯棉圆领T恤").productStatus(1).skuStatus(1).skuCode("TS-W-M")
                .specText("白 / M").price(new BigDecimal("79.00")).stock(100)
                .build();
    }

    private MallOrderSubmitDTO submitDTO() {
        MallOrderSubmitDTO dto = new MallOrderSubmitDTO();
        dto.setAddressId(1L);
        dto.setRemark("尽快发货");
        dto.setSubmitToken("tok-1");
        return dto;
    }

    // ---------------- 下单 ----------------

    @Test
    @DisplayName("下单：购物车没有选中商品 → 拒绝")
    void submitEmptyCart() {
        when(cartMapper.listSelectedByUser(USER)).thenReturn(List.of());
        assertThatThrownBy(() -> service.submit(submitDTO()))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.ORDER_CART_EMPTY);
        verify(stockService, never()).deductRedis(any());
    }

    @Test
    @DisplayName("下单：购物车有失效商品 → 报出是哪件商品")
    void submitInvalidItem() {
        MallCartItemVO invalid = cartItem(2);
        invalid.setProductStatus(0);   // 下架
        when(cartMapper.listSelectedByUser(USER)).thenReturn(List.of(invalid));

        assertThatThrownBy(() -> service.submit(submitDTO()))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("纯棉圆领T恤")
                .hasMessageContaining("商品已下架");
    }

    @Test
    @DisplayName("下单：DB 已有同 token 订单 → 拒绝（唯一索引兜底）")
    void submitDuplicateToken() {
        when(cartMapper.listSelectedByUser(USER)).thenReturn(List.of(cartItem(1)));
        when(orderMapper.countByUserAndToken(USER, "tok-1")).thenReturn(1);

        assertThatThrownBy(() -> service.submit(submitDTO()))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.ORDER_REPEAT_SUBMIT);
    }

    @Test
    @DisplayName("下单成功：金额服务端计算、快照收货信息、清购物车、预扣+DB扣库存")
    void submitSuccess() {
        when(cartMapper.listSelectedByUser(USER)).thenReturn(List.of(cartItem(2)));
        when(orderMapper.countByUserAndToken(USER, "tok-1")).thenReturn(0);

        MallOrderSubmitVO vo = service.submit(submitDTO());

        assertThat(vo.getTotalAmount()).isEqualByComparingTo("158.00");
        assertThat(vo.getPayAmount()).isEqualByComparingTo("158.00");
        assertThat(vo.getOrderNumber()).hasSize(20);
        assertThat(vo.getExpireTime()).isAfter(LocalDateTime.now());

        ArgumentCaptor<MallOrder> orderCap = ArgumentCaptor.forClass(MallOrder.class);
        verify(orderMapper).insert(orderCap.capture());
        MallOrder order = orderCap.getValue();
        assertThat(order.getStatus()).isEqualTo(MallOrderStatus.PENDING_PAYMENT.name());
        assertThat(order.getPayStatus()).isEqualTo(MallPayStatus.UN_PAID.name());
        assertThat(order.getReceiverName()).isEqualTo("张三");
        assertThat(order.getReceiverAreaCode()).isNull();
        assertThat(order.getRemark()).isEqualTo("尽快发货");
        assertThat(order.getSubmitToken()).isEqualTo("tok-1");

        // 明细快照 + 购物车清理
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MallOrderItem>> itemsCap = ArgumentCaptor.forClass(List.class);
        verify(orderItemMapper).batchInsert(itemsCap.capture());
        assertThat(itemsCap.getValue()).hasSize(1);
        assertThat(itemsCap.getValue().get(0).getTotalAmount()).isEqualByComparingTo("158.00");
        verify(cartMapper).deleteByIds(eq(USER), eq(List.of(5L)));

        // 两层防超卖都被调用
        verify(stockService).deductRedis(any());
        verify(stockService).deductDb(any());
        verify(stockService).refreshAggregates(any());
    }

    @Test
    @DisplayName("下单失败：DB 扣库存异常时回补 Redis")
    void submitReleasesRedisOnFailure() {
        when(cartMapper.listSelectedByUser(USER)).thenReturn(List.of(cartItem(1)));
        when(orderMapper.countByUserAndToken(USER, "tok-1")).thenReturn(0);
        org.mockito.Mockito.doThrow(new MallBusinessException(MallMessageConstant.STOCK_NOT_ENOUGH))
                .when(stockService).deductDb(any());

        assertThatThrownBy(() -> service.submit(submitDTO()))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.STOCK_NOT_ENOUGH);
        verify(stockService).releaseRedis(any());
        verify(orderMapper, never()).insert(any());
    }

    // ---------------- 取消 / 收货 ----------------

    @Test
    @DisplayName("取消：条件更新成功才回补库存")
    void cancelSuccess() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(MallOrder.builder()
                .id(9L).orderNumber(ORDER_NO).userId(USER)
                .status(MallOrderStatus.PENDING_PAYMENT.name()).build());
        when(orderMapper.cancelIfPendingPayment(eq(9L), any(), any())).thenReturn(1);
        when(orderItemMapper.listByOrderId(9L)).thenReturn(List.of(MallOrderItem.builder()
                .skuId(1L).productId(1L).number(2).build()));

        service.cancel(ORDER_NO);

        verify(stockService).releaseDb(any());
        verify(stockService).releaseRedis(any());
        verify(stockService).refreshAggregates(any());
    }

    @Test
    @DisplayName("取消：状态已变（条件更新 0 行）→ 报错，不重复回补")
    void cancelStatusError() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(MallOrder.builder()
                .id(9L).orderNumber(ORDER_NO).userId(USER)
                .status(MallOrderStatus.PENDING_PAYMENT.name()).build());
        when(orderMapper.cancelIfPendingPayment(eq(9L), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.cancel(ORDER_NO))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.ORDER_STATUS_ERROR);
        verify(stockService, never()).releaseDb(any());
    }

    @Test
    @DisplayName("确认收货：待收货 → 已完成")
    void confirmReceipt() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(MallOrder.builder()
                .id(9L).orderNumber(ORDER_NO).userId(USER)
                .status(MallOrderStatus.PENDING_RECEIPT.name()).build());
        when(orderMapper.transitionStatus(eq(9L), eq(MallOrderStatus.PENDING_RECEIPT.name()),
                eq(MallOrderStatus.COMPLETED.name()), any())).thenReturn(1);

        service.confirmReceipt(ORDER_NO);

        verify(orderMapper).transitionStatus(eq(9L), eq(MallOrderStatus.PENDING_RECEIPT.name()),
                eq(MallOrderStatus.COMPLETED.name()), any());
    }

    // ---------------- 支付 ----------------

    @Test
    @DisplayName("支付成功：状态流转成功才累加销量")
    void paySuccess() {
        when(orderMapper.markPaidIfPending(eq(ORDER_NO), any(), eq("MOCK"))).thenReturn(1);
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(MallOrder.builder().id(9L).orderNumber(ORDER_NO).build());
        when(orderItemMapper.listByOrderId(9L)).thenReturn(List.of(MallOrderItem.builder()
                .skuId(1L).productId(1L).number(2).build()));

        assertThat(service.paySuccess(ORDER_NO, "MOCK")).isTrue();
        verify(stockService).adjustSales(any(), eq(1));
    }

    @Test
    @DisplayName("支付幂等：订单已不是待付款 → 返回 false，不动销量")
    void paySuccessIdempotent() {
        when(orderMapper.markPaidIfPending(eq(ORDER_NO), any(), any())).thenReturn(0);
        assertThat(service.paySuccess(ORDER_NO, "MOCK")).isFalse();
        verify(stockService, never()).adjustSales(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    // ---------------- 发货 ----------------

    @Test
    @DisplayName("发货：待发货 → 待收货，并写物流单")
    void shipSuccess() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(MallOrder.builder()
                .id(9L).orderNumber(ORDER_NO).status(MallOrderStatus.PENDING_DELIVERY.name()).build());
        when(shipmentMapper.getByOrderId(9L)).thenReturn(null);
        when(orderMapper.transitionStatus(eq(9L), eq(MallOrderStatus.PENDING_DELIVERY.name()),
                eq(MallOrderStatus.PENDING_RECEIPT.name()), any())).thenReturn(1);

        MallShipDTO dto = new MallShipDTO();
        dto.setCompany("顺丰速运");
        dto.setTrackingNo("SF123456");
        service.ship(ORDER_NO, dto);

        ArgumentCaptor<MallShipment> cap = ArgumentCaptor.forClass(MallShipment.class);
        verify(shipmentMapper).insert(cap.capture());
        assertThat(cap.getValue().getCompany()).isEqualTo("顺丰速运");
        assertThat(cap.getValue().getTrackingNo()).isEqualTo("SF123456");
    }

    @Test
    @DisplayName("发货：已发货 → 拒绝")
    void shipAlreadyShipped() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(MallOrder.builder()
                .id(9L).orderNumber(ORDER_NO).status(MallOrderStatus.PENDING_RECEIPT.name()).build());
        when(shipmentMapper.getByOrderId(9L)).thenReturn(MallShipment.builder().id(1L).build());

        MallShipDTO dto = new MallShipDTO();
        dto.setCompany("顺丰");
        dto.setTrackingNo("X");
        assertThatThrownBy(() -> service.ship(ORDER_NO, dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.ORDER_ALREADY_SHIPPED);
    }
}
