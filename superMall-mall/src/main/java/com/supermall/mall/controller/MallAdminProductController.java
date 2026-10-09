package com.supermall.mall.controller;

import com.supermall.mall.dto.MallProductPageQueryDTO;
import com.supermall.mall.dto.MallProductSaveDTO;
import com.supermall.mall.service.MallProductService;
import com.supermall.mall.vo.MallProductVO;
import com.supermall.result.PageResult;
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

/**
 * 运营后台 - 商品管理。认证：请求头 mall-admin-token。
 */
@RestController
@RequestMapping("/api/mall/admin/products")
@Tag(name = "电商后台-商品")
@Slf4j
@RequiredArgsConstructor
public class MallAdminProductController {

    private final MallProductService mallProductService;

    @GetMapping("/page")
    @Operation(summary = "商品分页（传父分类会带上其后代分类的商品）")
    public Result<PageResult> page(MallProductPageQueryDTO query) {
        return Result.success(mallProductService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情（含多图 / 规格项 / SKU 列表）")
    public Result<MallProductVO> detail(@PathVariable Long id) {
        return Result.success(mallProductService.detail(id));
    }

    @PostMapping
    @Operation(summary = "新增商品（SPU + 多图 + 规格 + SKU 一次提交）")
    public Result<Long> create(@Valid @RequestBody MallProductSaveDTO dto) {
        dto.setId(null);
        return Result.success(mallProductService.save(dto));
    }

    @PutMapping
    @Operation(summary = "修改商品（同名集合整体覆盖；想清空某字段就传空字符串）")
    public Result<Long> update(@Valid @RequestBody MallProductSaveDTO dto) {
        return Result.success(mallProductService.save(dto));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "上下架：status=1 上架 / 0 下架")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        mallProductService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品（上架中的不允许删）")
    public Result<Void> remove(@PathVariable Long id) {
        mallProductService.remove(id);
        return Result.success();
    }
}
