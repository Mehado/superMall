package com.supermall.mall.controller;

import com.supermall.context.BaseContext;
import com.supermall.mall.dto.MallCartAddDTO;
import com.supermall.mall.dto.MallCartUpdateDTO;
import com.supermall.mall.service.MallCartService;
import com.supermall.mall.vo.MallCartAddVO;
import com.supermall.mall.vo.MallCartVO;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 商城买家 - 购物车。认证：请求头 token。
 *
 * <p>userId 一律从登录态取，不接受前端传。
 */
@RestController
@RequestMapping("/api/mall/user/cart")
@Tag(name = "电商-购物车")
@Slf4j
@RequiredArgsConstructor
public class MallUserCartController {

    private final MallCartService mallCartService;

    @GetMapping
    @Operation(summary = "购物车列表 + 汇总",
            description = """
                    价格实时取自 mall_sku，不是加购时快照。
                    失效商品（商品下架 / 规格失效 / 售罄 / 库存不足）照常返回，
                    带 valid=false 与 invalidReason，但不计入 selectedAmount、也不能选中。
                    """)
    public Result<MallCartVO> get() {
        return Result.success(mallCartService.get(BaseContext.getCurrentId()));
    }

    @GetMapping("/count")
    @Operation(summary = "购物车角标数量", description = "返回 { total, invalid }")
    public Result<Map<String, Integer>> count() {
        Long userId = BaseContext.getCurrentId();
        return Result.success(Map.of(
                "total", mallCartService.count(userId),
                "invalid", mallCartService.invalidCount(userId)));
    }

    @PostMapping
    @Operation(summary = "加入购物车",
            description = "同一 SKU 累加；累加结果会被库存上限截断，返回最终数量。商品下架/规格停用/售罄会被拒绝。")
    public Result<MallCartAddVO> add(@Valid @RequestBody MallCartAddDTO dto) {
        return Result.success(mallCartService.add(BaseContext.getCurrentId(), dto.getSkuId(), dto.getNumber()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改数量", description = "数量不能超过当前库存")
    public Result<Void> updateNumber(@PathVariable Long id, @Valid @RequestBody MallCartUpdateDTO dto) {
        mallCartService.updateNumber(BaseContext.getCurrentId(), id, dto.getNumber());
        return Result.success();
    }

    @PutMapping("/{id}/selected")
    @Operation(summary = "勾选 / 取消勾选单行", description = "selected=1 选中，0 取消；失效商品不能选中")
    public Result<Void> updateSelected(@PathVariable Long id, @RequestParam Integer selected) {
        mallCartService.updateSelected(BaseContext.getCurrentId(), id, selected);
        return Result.success();
    }

    @PutMapping("/selected")
    @Operation(summary = "全选 / 全不选",
            description = "selected=1 时只勾选有效商品（失效的不会被带上）；selected=0 时全部取消")
    public Result<Void> updateAllSelected(@RequestParam Integer selected) {
        mallCartService.updateAllSelected(BaseContext.getCurrentId(), selected);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除单行（幂等）")
    public Result<Void> remove(@PathVariable Long id) {
        mallCartService.remove(BaseContext.getCurrentId(), id);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "清空购物车 / 清理失效商品",
            description = "scope=all（默认）清空全部；scope=invalid 只清失效商品")
    public Result<Void> clear(@RequestParam(required = false) String scope) {
        mallCartService.clear(BaseContext.getCurrentId(), scope);
        return Result.success();
    }
}
