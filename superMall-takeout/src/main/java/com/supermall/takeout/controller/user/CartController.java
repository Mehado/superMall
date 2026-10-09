package com.supermall.takeout.controller.user;

import com.supermall.takeout.dto.CartAddDTO;
import com.supermall.takeout.dto.CartUpdateDTO;
import com.supermall.result.Result;
import com.supermall.takeout.service.CartService;
import com.supermall.takeout.vo.CartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/cart")
@Tag(name = "用户端-购物车")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "查看购物车（按商家分组）")
    public Result<List<CartVO>> list() {
        return Result.success(cartService.list());
    }

    @PostMapping
    @Operation(summary = "加入购物车")
    public Result<Void> add(@Valid @RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改购物车数量（0 表示删除）")
    public Result<Void> updateNumber(@Valid @RequestBody CartUpdateDTO dto) {
        cartService.updateNumber(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除购物车商品")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.deleteItem(id);
        return Result.success();
    }

    @DeleteMapping("/merchant/{merchantId}")
    @Operation(summary = "清空某商家购物车")
    public Result<Void> clearByMerchant(@PathVariable Long merchantId) {
        cartService.clearByMerchant(merchantId);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "清空购物车")
    public Result<Void> clearAll() {
        cartService.clearAll();
        return Result.success();
    }
}
