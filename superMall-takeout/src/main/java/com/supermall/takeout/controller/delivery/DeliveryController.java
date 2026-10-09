package com.supermall.takeout.controller.delivery;

import com.supermall.takeout.annotations.NoRepeatSubmit;
import com.supermall.takeout.dto.DeliveryPageQueryDTO;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import com.supermall.takeout.service.DeliveryService;
import com.supermall.takeout.vo.DeliveryOrderVO;
import com.supermall.takeout.vo.DeliveryStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery/orders")
@Tag(name = "配送端-配送单")
@Slf4j
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/pool")
    @Operation(summary = "待抢单池")
    public Result<PageResult> pool(@Valid DeliveryPageQueryDTO dto) {
        return Result.success(deliveryService.pool(dto));
    }

    @NoRepeatSubmit
    @PutMapping("/grab/{orderId}")
    @Operation(summary = "抢单")
    public Result<Void> grab(@PathVariable Long orderId) {
        deliveryService.grab(orderId);
        return Result.success();
    }

    @GetMapping("/mine")
    @Operation(summary = "我的配送单")
    public Result<PageResult> mine(@Valid DeliveryPageQueryDTO dto) {
        return Result.success(deliveryService.myOrders(dto));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "配送单详情")
    public Result<DeliveryOrderVO> detail(@PathVariable Long orderId) {
        return Result.success(deliveryService.details(orderId));
    }

    @NoRepeatSubmit
    @PutMapping("/pickup/{orderId}")
    @Operation(summary = "取餐，开始配送")
    public Result<Void> pickup(@PathVariable Long orderId) {
        deliveryService.pickup(orderId);
        return Result.success();
    }

    @NoRepeatSubmit
    @PutMapping("/delivered/{orderId}")
    @Operation(summary = "确认送达")
    public Result<Void> delivered(@PathVariable Long orderId) {
        deliveryService.delivered(orderId);
        return Result.success();
    }

    @GetMapping("/statistics")
    @Operation(summary = "配送端统计")
    public Result<DeliveryStatisticsVO> statistics() {
        return Result.success(deliveryService.statistics());
    }
}
