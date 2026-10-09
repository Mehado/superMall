package com.supermall.mall.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallRefundApplyDTO;
import com.supermall.mall.dto.MallRefundAuditDTO;
import com.supermall.mall.entity.MallOrder;
import com.supermall.mall.entity.MallOrderItem;
import com.supermall.mall.entity.MallRefund;
import com.supermall.mall.enumeration.MallOrderStatus;
import com.supermall.mall.enumeration.MallPayStatus;
import com.supermall.mall.enumeration.MallRefundStatus;
import com.supermall.mall.enumeration.MallRefundType;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallOrderItemMapper;
import com.supermall.mall.mapper.MallOrderMapper;
import com.supermall.mall.mapper.MallRefundMapper;
import com.supermall.mall.service.MallOrderService;
import com.supermall.mall.service.MallRefundService;
import com.supermall.mall.service.MallStockService;
import com.supermall.mall.vo.MallRefundVO;
import com.supermall.result.PageResult;
import com.supermall.utils.RichTextSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 退款申请与审核实现。
 *
 * <p>并发安全靠「订单状态条件更新」：
 * 申请时 {@code 可退款状态 -> REFUNDING}，只有一次能成功；
 * 审核时 {@code refund.status='PENDING' -> 新状态}，只有一次能成功。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallRefundServiceImpl implements MallRefundService {

    private static final DateTimeFormatter REFUND_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final MallRefundMapper mallRefundMapper;
    private final MallOrderMapper mallOrderMapper;
    private final MallOrderItemMapper mallOrderItemMapper;
    private final MallStockService mallStockService;
    private final MallOrderService mallOrderService;

    // ============================================================
    // 买家
    // ============================================================

    @Override
    @Transactional
    public MallRefundVO apply(Long userId, String orderNumber, MallRefundApplyDTO dto) {
        if (!MallRefundType.isValid(dto.getType())) {
            throw new MallBusinessException(MallMessageConstant.REFUND_TYPE_INVALID);
        }
        MallOrder order = mallOrderMapper.getByNumber(orderNumber);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new MallBusinessException(MallMessageConstant.ORDER_NOT_FOUND);
        }
        String previous = order.getStatus();
        if (!MallOrderStatus.isRefundable(previous)) {
            throw new MallBusinessException(MallMessageConstant.REFUND_NOT_ALLOWED);
        }

        // 条件流转即互斥锁：并发重复申请只有一个能成功
        int rows = mallOrderMapper.transitionStatus(order.getId(), previous,
                MallOrderStatus.REFUNDING.name(), MallOrder.builder().build());
        if (rows == 0) {
            throw new MallBusinessException(MallMessageConstant.REFUND_NOT_ALLOWED);
        }

        LocalDateTime now = LocalDateTime.now();
        MallRefund refund = MallRefund.builder()
                .refundNumber(generateRefundNumber())
                .orderId(order.getId())
                .orderNumber(orderNumber)
                .userId(userId)
                .type(dto.getType())
                .reason(dto.getReason().trim())
                .description(RichTextSanitizer.plainText(dto.getDescription()))
                .amount(order.getPayAmount())
                .status(MallRefundStatus.PENDING.name())
                .previousStatus(previous)
                .createTime(now)
                .updateTime(now)
                .build();
        mallRefundMapper.insert(refund);
        log.info("买家申请退款：refundNumber={}, orderNumber={}, type={}, amount={}",
                refund.getRefundNumber(), orderNumber, dto.getType(), refund.getAmount());
        return toVO(refund, null);
    }

    @Override
    public MallRefundVO detailForUser(Long userId, String orderNumber) {
        MallOrder order = mallOrderMapper.getByNumber(orderNumber);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new MallBusinessException(MallMessageConstant.ORDER_NOT_FOUND);
        }
        MallRefund refund = mallRefundMapper.getByOrderId(order.getId());
        if (refund == null) {
            throw new MallBusinessException(MallMessageConstant.REFUND_NOT_FOUND);
        }
        return toVO(refund, null);
    }

    // ============================================================
    // 运营后台
    // ============================================================

    @Override
    public PageResult pageForAdmin(String status, Integer page, Integer pageSize) {
        int p = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        PageHelper.startPage(p, size);
        Page<MallRefund> result = mallRefundMapper.page(status);
        return new PageResult(result.getTotal(), toVOList(result.getResult()));
    }

    @Override
    public MallRefundVO detailForAdmin(Long id) {
        MallRefund refund = requireRefund(id);
        return toVO(refund, mallOrderService.detailForAdmin(refund.getOrderNumber()));
    }

    @Override
    @Transactional
    public void audit(Long id, MallRefundAuditDTO dto) {
        MallRefund refund = requireRefund(id);
        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        String newStatus = approved ? MallRefundStatus.APPROVED.name() : MallRefundStatus.REJECTED.name();

        int rows = mallRefundMapper.audit(id, newStatus, dto.getAdminRemark(), LocalDateTime.now());
        if (rows == 0) {
            throw new MallBusinessException(MallMessageConstant.REFUND_STATUS_ERROR);
        }

        MallOrder order = mallOrderMapper.getById(refund.getOrderId());
        if (approved) {
            // 订单 REFUNDING -> REFUNDED，pay_status -> REFUNDED
            int r = mallOrderMapper.transitionStatus(order.getId(),
                    MallOrderStatus.REFUNDING.name(),
                    MallOrderStatus.REFUNDED.name(),
                    MallOrder.builder().payStatus(MallPayStatus.REFUNDED.name()).build());
            if (r == 0) {
                throw new MallBusinessException(MallMessageConstant.ORDER_STATUS_ERROR);
            }
            // 回补库存 + 扣回销量
            List<MallOrderItem> items = mallOrderItemMapper.listByOrderId(order.getId());
            mallStockService.releaseDb(items);
            mallStockService.refreshAggregates(items);
            mallStockService.releaseRedis(items);
            mallStockService.adjustSales(items, -1);
            log.info("退款审核通过：refundId={}, orderNumber={}, 已回补库存并扣回销量", id, order.getOrderNumber());
        } else {
            // 驳回：订单回到申请前的状态
            int r = mallOrderMapper.transitionStatus(order.getId(),
                    MallOrderStatus.REFUNDING.name(),
                    refund.getPreviousStatus(),
                    MallOrder.builder().build());
            if (r == 0) {
                throw new MallBusinessException(MallMessageConstant.ORDER_STATUS_ERROR);
            }
            log.info("退款审核驳回：refundId={}, orderNumber={}, 回退到 {}",
                    id, order.getOrderNumber(), refund.getPreviousStatus());
        }
    }

    // ============================================================
    // 私有
    // ============================================================

    private MallRefund requireRefund(Long id) {
        MallRefund refund = id == null ? null : mallRefundMapper.getById(id);
        if (refund == null) {
            throw new MallBusinessException(MallMessageConstant.REFUND_NOT_FOUND);
        }
        return refund;
    }

    private List<MallRefundVO> toVOList(List<MallRefund> refunds) {
        List<MallRefundVO> list = new ArrayList<>();
        for (MallRefund r : refunds) {
            list.add(toVO(r, null));
        }
        return list;
    }

    private MallRefundVO toVO(MallRefund r, com.supermall.mall.vo.MallOrderVO order) {
        MallRefundVO vo = new MallRefundVO();
        BeanUtils.copyProperties(r, vo);
        vo.setTypeDesc(MallRefundType.descOf(r.getType()));
        vo.setStatusDesc(MallRefundStatus.descOf(r.getStatus()));
        vo.setOrder(order);
        return vo;
    }

    private String generateRefundNumber() {
        return "R" + LocalDateTime.now().format(REFUND_NO_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}
