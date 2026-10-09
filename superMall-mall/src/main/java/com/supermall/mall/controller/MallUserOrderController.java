package com.supermall.mall.controller;

import com.supermall.context.BaseContext;
import com.supermall.mall.dto.MallOrderSubmitDTO;
import com.supermall.mall.dto.MallRefundApplyDTO;
import com.supermall.mall.service.MallOrderService;
import com.supermall.mall.service.MallRefundService;
import com.supermall.mall.vo.MallOrderSubmitVO;
import com.supermall.mall.vo.MallOrderVO;
import com.supermall.mall.vo.MallRefundVO;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商城买家 - 订单。认证：请求头 token。
 */
@RestController
@RequestMapping("/api/mall/user/orders")
@Tag(name = "电商-买家订单")
@Slf4j
@RequiredArgsConstructor
public class MallUserOrderController {

    private final MallOrderService mallOrderService;
    private final MallRefundService mallRefundService;

    @GetMapping("/token")
    @Operation(summary = "获取下单令牌", description = "10 分钟有效、一次性；提交订单时原样回传 submitToken")
    public Result<Map<String, String>> token() {
        return Result.success(Map.of("submitToken", mallOrderService.getSubmitToken()));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交订单",
            description = "结算购物车里「有效且选中」的商品。金额由服务端重算；库存 Redis 预扣 + DB 条件扣减双重防超卖。")
    public Result<MallOrderSubmitVO> submit(@Valid @RequestBody MallOrderSubmitDTO dto) {
        return Result.success(mallOrderService.submit(dto));
    }

    @GetMapping("/page")
    @Operation(summary = "我的订单分页", description = "status 可选：PENDING_PAYMENT / PENDING_DELIVERY / PENDING_RECEIPT / COMPLETED / CANCELLED / REFUNDING / REFUNDED")
    public Result<PageResult> page(@RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(mallOrderService.pageForUser(status, page, pageSize));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "订单详情（含明细 / 物流 / 退款进度）")
    public Result<MallOrderVO> detail(@PathVariable String orderNumber) {
        return Result.success(mallOrderService.detailForUser(orderNumber));
    }

    @PutMapping("/{orderNumber}/cancel")
    @Operation(summary = "取消订单", description = "只有待付款可取消；取消后回补库存")
    public Result<Void> cancel(@PathVariable String orderNumber) {
        mallOrderService.cancel(orderNumber);
        return Result.success();
    }

    @PutMapping("/{orderNumber}/receipt")
    @Operation(summary = "确认收货", description = "待收货 → 已完成")
    public Result<Void> receipt(@PathVariable String orderNumber) {
        mallOrderService.confirmReceipt(orderNumber);
        return Result.success();
    }

    @PostMapping("/{orderNumber}/refund")
    @Operation(summary = "申请退款",
            description = "待发货 / 待收货 / 已完成 可申请；一单同时只能有一个进行中的退款申请")
    public Result<MallRefundVO> applyRefund(@PathVariable String orderNumber,
                                            @Valid @RequestBody MallRefundApplyDTO dto) {
        return Result.success(mallRefundService.apply(BaseContext.getCurrentId(), orderNumber, dto));
    }

    @GetMapping("/{orderNumber}/refund")
    @Operation(summary = "查看该订单的退款进度")
    public Result<MallRefundVO> refund(@PathVariable String orderNumber) {
        return Result.success(mallRefundService.detailForUser(BaseContext.getCurrentId(), orderNumber));
    }
}
