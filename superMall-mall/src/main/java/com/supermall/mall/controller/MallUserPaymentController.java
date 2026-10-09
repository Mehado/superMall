package com.supermall.mall.controller;

import com.supermall.mall.dto.MallPaymentDTO;
import com.supermall.mall.service.MallPaymentService;
import com.supermall.mall.vo.MallPaymentResultVO;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商城买家 - 支付（当前为 Mock）。认证：请求头 token。
 */
@RestController
@RequestMapping("/api/mall/user/payment")
@Tag(name = "电商-支付")
@Slf4j
@RequiredArgsConstructor
public class MallUserPaymentController {

    private final MallPaymentService mallPaymentService;

    @GetMapping("/token")
    @Operation(summary = "获取支付令牌", description = "5 分钟有效、一次性；只有待付款订单能获取")
    public Result<Map<String, String>> token(@RequestParam String orderNumber) {
        return Result.success(Map.of("payToken", mallPaymentService.getPayToken(orderNumber)));
    }

    @PostMapping("/pay")
    @Operation(summary = "支付（Mock，点一下即成功）",
            description = "成功后订单进入「待发货」，并把销量累加到 SKU 与 SPU")
    public Result<MallPaymentResultVO> pay(@Valid @RequestBody MallPaymentDTO dto) {
        return Result.success(mallPaymentService.pay(dto));
    }
}
