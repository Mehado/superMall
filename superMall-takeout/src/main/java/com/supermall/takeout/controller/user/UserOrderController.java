package com.supermall.takeout.controller.user;

import com.supermall.takeout.annotations.NoRepeatSubmit;
import com.supermall.takeout.annotations.RateLimit;
import com.supermall.takeout.dto.OrderSubmitDTO;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import com.supermall.takeout.service.OrderService;
import com.supermall.takeout.vo.OrderSubmitVO;
import com.supermall.takeout.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/orders")
@Tag(name = "用户端-订单")
@Slf4j
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderService orderService;

    @RateLimit(limit = 30, window = 60)
    @GetMapping("/token")
    @Operation(summary = "获取下单幂等令牌")
    public Result<String> submitToken() {
        return Result.success(orderService.getSubmitToken());
    }

    @RateLimit(limit = 15, window = 60)
    @PostMapping("/submit")
    @Operation(summary = "用户下单")
    public Result<OrderSubmitVO> submit(@Valid @RequestBody OrderSubmitDTO dto) {
        log.info("订单提交：{}", dto);
        return Result.success(orderService.submitOrder(dto));
    }

    @GetMapping
    @Operation(summary = "我的订单分页")
    public Result<PageResult> page(int page, int pageSize, String status) {
        return Result.success(orderService.pageQuery4User(page, pageSize, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public Result<OrderVO> details(@PathVariable Long id) {
        return Result.success(orderService.details(id));
    }

    @NoRepeatSubmit
    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消订单")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.userCancel(id);
        return Result.success();
    }
}
