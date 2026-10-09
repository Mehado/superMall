package com.supermall.takeout.controller.merchant;

import com.supermall.takeout.annotations.NoRepeatSubmit;
import com.supermall.takeout.dto.OrderCancelDTO;
import com.supermall.takeout.dto.OrderPageQueryDTO;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import com.supermall.takeout.service.OrderService;
import com.supermall.takeout.vo.OrderStatisticsVO;
import com.supermall.takeout.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant/orders")
@Tag(name = "商家端-订单管理")
@Slf4j
@RequiredArgsConstructor
public class MerchantOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "订单分页查询")
    public Result<PageResult> page(@Valid OrderPageQueryDTO dto) {
        return Result.success(orderService.pageQuery4Merchant(dto));
    }

    @GetMapping("/statistics")
    @Operation(summary = "各状态订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        return Result.success(orderService.statistics());
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.merchantDetails(id));
    }

    @NoRepeatSubmit
    @PutMapping("/accept/{id}")
    @Operation(summary = "接单")
    public Result<Void> accept(@PathVariable Long id) {
        orderService.merchantAccept(id);
        return Result.success();
    }

    @NoRepeatSubmit
    @PutMapping("/ready/{id}")
    @Operation(summary = "备餐完成，进入待配送")
    public Result<Void> ready(@PathVariable Long id) {
        orderService.merchantReady(id);
        return Result.success();
    }

    @NoRepeatSubmit
    @PutMapping("/reject")
    @Operation(summary = "拒单")
    public Result<Void> reject(@Valid @RequestBody OrderCancelDTO dto) {
        orderService.merchantReject(dto.getId(), dto.getCancelReason());
        return Result.success();
    }

    @PutMapping("/simulate-pay/{orderNumber}")
    @Operation(summary = "模拟支付成功（开发调试用）")
    public Result<Void> simulatePay(@PathVariable String orderNumber) {
        log.info("模拟支付成功：{}", orderNumber);
        orderService.paySuccess(orderNumber, "MOCK");
        return Result.success();
    }
}
