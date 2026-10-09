package com.supermall.takeout.controller.delivery;

import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.DeliverymanLoginDTO;
import com.supermall.takeout.dto.DeliverymanRegisterDTO;
import com.supermall.takeout.dto.DeliverymanUpdateDTO;
import com.supermall.takeout.entity.Deliveryman;
import com.supermall.result.Result;
import com.supermall.takeout.service.DeliverymanService;
import com.supermall.takeout.vo.DeliveryLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery")
@Tag(name = "配送端-账号")
@RequiredArgsConstructor
public class DeliverymanController {

    private final DeliverymanService deliverymanService;

    @PostMapping("/login")
    @Operation(summary = "配送员登录")
    public Result<DeliveryLoginVO> login(@Valid @RequestBody DeliverymanLoginDTO dto) {
        return Result.success(deliverymanService.login(dto));
    }

    @PostMapping("/register")
    @Operation(summary = "骑手注册（公开）",
            description = "注册即启用并直接返回 token（注册即登录）；需带图形验证码")
    public Result<DeliveryLoginVO> register(@Valid @RequestBody DeliverymanRegisterDTO dto) {
        return Result.success(deliverymanService.register(dto));
    }

    @GetMapping("/profile")
    @Operation(summary = "查询配送员资料")
    public Result<Deliveryman> profile() {
        return Result.success(deliverymanService.getById(BaseContext.getCurrentId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "修改配送员资料")
    public Result<Void> updateProfile(@Valid @RequestBody DeliverymanUpdateDTO dto) {
        deliverymanService.updateProfile(dto);
        return Result.success();
    }
}
