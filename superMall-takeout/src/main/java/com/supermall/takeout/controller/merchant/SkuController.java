package com.supermall.takeout.controller.merchant;

import com.supermall.takeout.entity.Sku;
import com.supermall.result.Result;
import com.supermall.takeout.service.SkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/skus")
@Tag(name = "商家端-商品规格")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @GetMapping("/product/{productId}")
    @Operation(summary = "某商品的规格列表")
    public Result<List<Sku>> listByProduct(@PathVariable Long productId) {
        return Result.success(skuService.listByProductId(productId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "规格详情")
    public Result<Sku> detail(@PathVariable Long id) {
        return Result.success(skuService.getById(id));
    }
}
