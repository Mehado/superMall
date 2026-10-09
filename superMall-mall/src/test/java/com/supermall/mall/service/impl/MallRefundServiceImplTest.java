package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallRefundApplyDTO;
import com.supermall.mall.dto.MallRefundAuditDTO;
import com.supermall.mall.entity.MallOrder;
import com.supermall.mall.entity.MallOrderItem;
import com.supermall.mall.entity.MallRefund;
import com.supermall.mall.enumeration.MallOrderStatus;
import com.supermall.mall.enumeration.MallPayStatus;
import com.supermall.mall.enumeration.MallRefundStatus;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallOrderItemMapper;
import com.supermall.mall.mapper.MallOrderMapper;
import com.supermall.mall.mapper.MallRefundMapper;
import com.supermall.mall.service.MallOrderService;
import com.supermall.mall.service.MallStockService;
import com.supermall.mall.vo.MallRefundVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 退款申请与审核：状态互斥、同意回补库存+扣回销量、驳回回退状态。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallRefundServiceImpl - 退货退款")
class MallRefundServiceImplTest {

    private static final Long USER = 7L;
    private static final String ORDER_NO = "20260920120000001123";

    @Mock private MallRefundMapper refundMapper;
    @Mock private MallOrderMapper orderMapper;
    @Mock private MallOrderItemMapper orderItemMapper;
    @Mock private MallStockService stockService;
    @Mock private MallOrderService orderService;

    private MallRefundServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MallRefundServiceImpl(refundMapper, orderMapper, orderItemMapper,
                stockService, orderService);
        lenient().doAnswer(inv -> {
            inv.getArgument(0, MallRefund.class).setId(11L);
            return null;
        }).when(refundMapper).insert(any(MallRefund.class));
    }

    private MallOrder paidOrder(String status) {
        return MallOrder.builder()
                .id(9L).orderNumber(ORDER_NO).userId(USER).status(status)
                .payStatus(MallPayStatus.PAID.name())
                .payAmount(new BigDecimal("158.00"))
                .build();
    }

    private MallRefundApplyDTO applyDTO() {
        MallRefundApplyDTO dto = new MallRefundApplyDTO();
        dto.setType(2);
        dto.setReason("不想要了");
        dto.setDescription("已拆封");
        return dto;
    }

    // ---------------- 申请 ----------------

    @Test
    @DisplayName("申请：退款类型非法 → 拒绝")
    void applyInvalidType() {
        MallRefundApplyDTO dto = applyDTO();
        dto.setType(9);
        assertThatThrownBy(() -> service.apply(USER, ORDER_NO, dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.REFUND_TYPE_INVALID);
    }

    @Test
    @DisplayName("申请：订单不属于自己 → 订单不存在")
    void applyNotOwned() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(MallOrder.builder()
                .id(9L).orderNumber(ORDER_NO).userId(999L)
                .status(MallOrderStatus.PENDING_RECEIPT.name()).build());
        assertThatThrownBy(() -> service.apply(USER, ORDER_NO, applyDTO()))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("申请：待付款订单不能退款")
    void applyNotRefundable() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(paidOrder(MallOrderStatus.PENDING_PAYMENT.name()));
        assertThatThrownBy(() -> service.apply(USER, ORDER_NO, applyDTO()))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.REFUND_NOT_ALLOWED);
        verify(refundMapper, never()).insert(any());
    }

    @Test
    @DisplayName("申请：状态条件更新失败（并发重复申请）→ 拒绝")
    void applyConcurrentDuplicate() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(paidOrder(MallOrderStatus.PENDING_RECEIPT.name()));
        when(orderMapper.transitionStatus(eq(9L), eq(MallOrderStatus.PENDING_RECEIPT.name()),
                eq(MallOrderStatus.REFUNDING.name()), any())).thenReturn(0);
        assertThatThrownBy(() -> service.apply(USER, ORDER_NO, applyDTO()))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.REFUND_NOT_ALLOWED);
    }

    @Test
    @DisplayName("申请成功：订单置为退款中，退款单记录申请前状态与实付金额")
    void applySuccess() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(paidOrder(MallOrderStatus.PENDING_RECEIPT.name()));
        when(orderMapper.transitionStatus(eq(9L), eq(MallOrderStatus.PENDING_RECEIPT.name()),
                eq(MallOrderStatus.REFUNDING.name()), any())).thenReturn(1);

        MallRefundVO vo = service.apply(USER, ORDER_NO, applyDTO());

        assertThat(vo.getStatus()).isEqualTo(MallRefundStatus.PENDING.name());
        assertThat(vo.getPreviousStatus()).isEqualTo(MallOrderStatus.PENDING_RECEIPT.name());
        assertThat(vo.getAmount()).isEqualByComparingTo("158.00");
        assertThat(vo.getTypeDesc()).isEqualTo("退货退款");

        ArgumentCaptor<MallRefund> cap = ArgumentCaptor.forClass(MallRefund.class);
        verify(refundMapper).insert(cap.capture());
        assertThat(cap.getValue().getRefundNumber()).startsWith("R");
    }

    // ---------------- 审核 ----------------

    @Test
    @DisplayName("审核通过：订单→已退款，回补库存并扣回销量")
    void auditApproved() {
        when(refundMapper.getById(11L)).thenReturn(MallRefund.builder()
                .id(11L).orderId(9L).orderNumber(ORDER_NO).userId(USER)
                .status(MallRefundStatus.PENDING.name())
                .previousStatus(MallOrderStatus.PENDING_RECEIPT.name()).build());
        when(refundMapper.audit(eq(11L), eq(MallRefundStatus.APPROVED.name()), any(), any())).thenReturn(1);
        when(orderMapper.getById(9L)).thenReturn(paidOrder(MallOrderStatus.REFUNDING.name()));
        when(orderMapper.transitionStatus(eq(9L), eq(MallOrderStatus.REFUNDING.name()),
                eq(MallOrderStatus.REFUNDED.name()), any())).thenReturn(1);
        when(orderItemMapper.listByOrderId(9L)).thenReturn(List.of(MallOrderItem.builder()
                .skuId(1L).productId(1L).number(2).build()));

        MallRefundAuditDTO dto = new MallRefundAuditDTO();
        dto.setApproved(true);
        dto.setAdminRemark("同意");
        service.audit(11L, dto);

        verify(stockService).releaseDb(any());
        verify(stockService).releaseRedis(any());
        verify(stockService).adjustSales(any(), eq(-1));
        verify(orderMapper).transitionStatus(eq(9L), eq(MallOrderStatus.REFUNDING.name()),
                eq(MallOrderStatus.REFUNDED.name()), any());
    }

    @Test
    @DisplayName("审核驳回：订单回退到申请前状态，不动库存")
    void auditRejected() {
        when(refundMapper.getById(11L)).thenReturn(MallRefund.builder()
                .id(11L).orderId(9L).orderNumber(ORDER_NO).userId(USER)
                .status(MallRefundStatus.PENDING.name())
                .previousStatus(MallOrderStatus.PENDING_DELIVERY.name()).build());
        when(refundMapper.audit(eq(11L), eq(MallRefundStatus.REJECTED.name()), any(), any())).thenReturn(1);
        when(orderMapper.getById(9L)).thenReturn(paidOrder(MallOrderStatus.REFUNDING.name()));
        when(orderMapper.transitionStatus(eq(9L), eq(MallOrderStatus.REFUNDING.name()),
                eq(MallOrderStatus.PENDING_DELIVERY.name()), any())).thenReturn(1);

        MallRefundAuditDTO dto = new MallRefundAuditDTO();
        dto.setApproved(false);
        dto.setAdminRemark("不符合条件");
        service.audit(11L, dto);

        verify(stockService, never()).releaseDb(any());
        verify(stockService, never()).adjustSales(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("审核：已审核过的退款单 → 拒绝重复审核")
    void auditAlreadyDone() {
        when(refundMapper.getById(11L)).thenReturn(MallRefund.builder()
                .id(11L).orderId(9L).orderNumber(ORDER_NO)
                .status(MallRefundStatus.APPROVED.name()).build());
        when(refundMapper.audit(eq(11L), any(), any(), any())).thenReturn(0);

        MallRefundAuditDTO dto = new MallRefundAuditDTO();
        dto.setApproved(true);
        assertThatThrownBy(() -> service.audit(11L, dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.REFUND_STATUS_ERROR);
    }
}
