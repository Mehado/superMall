package com.supermall.takeout.controller.merchant;

import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.MerchantApplyDTO;
import com.supermall.takeout.dto.MerchantBusinessDTO;
import com.supermall.takeout.dto.MerchantLoginDTO;
import com.supermall.takeout.dto.MerchantUpdateDTO;
import com.supermall.takeout.entity.Merchant;
import com.supermall.result.Result;
import com.supermall.takeout.service.MerchantService;
import com.supermall.takeout.vo.MerchantApplyVO;
import com.supermall.takeout.vo.MerchantLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant")
@Tag(name = "商家端-账号与店铺")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/login")
    @Operation(summary = "商家登录", description = "入驻申请待审核 / 已驳回的账号会被拦下并给出原因")
    public Result<MerchantLoginVO> login(@Valid @RequestBody MerchantLoginDTO dto) {
        return Result.success(merchantService.login(dto));
    }

    @PostMapping("/apply")
    @Operation(summary = "商家入驻申请（公开）",
            description = "提交后进入待审核，平台运营审核通过才能登录商家端；需带图形验证码")
    public Result<MerchantApplyVO> apply(@Valid @RequestBody MerchantApplyDTO dto) {
        return Result.success(merchantService.apply(dto));
    }

    @GetMapping("/apply/status")
    @Operation(summary = "查询入驻申请状态（公开）", description = "按邮箱查；返回待审核 / 已通过 / 已驳回及原因")
    public Result<MerchantApplyVO> applyStatus(@RequestParam String username) {
        return Result.success(merchantService.applyStatus(username));
    }

    @GetMapping("/shop")
    @Operation(summary = "查询店铺信息")
    public Result<Merchant> shop() {
        Merchant merchant = merchantService.getById(BaseContext.getCurrentId());
        if (merchant != null) {
            merchant.setPassword(null);
        }
        return Result.success(merchant);
    }

    @PutMapping("/shop")
    @Operation(summary = "修改店铺信息")
    public Result<Void> updateShop(@Valid @RequestBody MerchantUpdateDTO dto) {
        merchantService.updateShop(dto);
        return Result.success();
    }

    @GetMapping("/shop/business")
    @Operation(summary = "查询营业状态（1营业中 0休息中）")
    public Result<Integer> businessStatus() {
        return Result.success(merchantService.getBusinessStatus(BaseContext.getCurrentId()));
    }

    @PutMapping("/shop/business")
    @Operation(summary = "设置营业状态（1营业中 0休息中）")
    public Result<Void> updateBusinessStatus(@Valid @RequestBody MerchantBusinessDTO dto) {
        merchantService.updateBusinessStatus(BaseContext.getCurrentId(), dto.getBusinessStatus());
        return Result.success();
    }
}
