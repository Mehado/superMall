package com.supermall.takeout.controller.user;

import com.supermall.takeout.annotations.NoRepeatSubmit;
import com.supermall.takeout.dto.PaymentDTO;
import com.supermall.result.Result;
import com.supermall.takeout.service.PaymentService;
import com.supermall.takeout.vo.OrderPaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/payment")
@Tag(name = "用户端-支付")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/token")
    @Operation(summary = "获取支付幂等令牌")
    public Result<String> payToken(@RequestParam String orderNumber) {
        return Result.success(paymentService.getPayToken(orderNumber));
    }

    @NoRepeatSubmit
    @PostMapping("/pay")
    @Operation(summary = "发起支付（Mock）")
    public Result<OrderPaymentVO> pay(@Valid @RequestBody PaymentDTO dto) {
        return Result.success(paymentService.pay(dto));
    }
}
