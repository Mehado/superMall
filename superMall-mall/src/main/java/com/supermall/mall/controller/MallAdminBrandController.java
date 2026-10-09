package com.supermall.mall.controller;

import com.supermall.mall.dto.MallBrandDTO;
import com.supermall.mall.entity.MallBrand;
import com.supermall.mall.service.MallBrandService;
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

import java.util.List;

/**
 * 运营后台 - 品牌管理。认证：请求头 mall-admin-token。
 */
@RestController
@RequestMapping("/api/mall/admin/brands")
@Tag(name = "电商后台-品牌")
@Slf4j
@RequiredArgsConstructor
public class MallAdminBrandController {

    private final MallBrandService mallBrandService;

    @GetMapping
    @Operation(summary = "品牌列表（名称模糊 + 状态过滤）")
    public Result<List<MallBrand>> list(@RequestParam(required = false) String name,
                                       @RequestParam(required = false) Integer status) {
        return Result.success(mallBrandService.list(name, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "品牌详情")
    public Result<MallBrand> detail(@PathVariable Long id) {
        return Result.success(mallBrandService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增品牌")
    public Result<Long> create(@Valid @RequestBody MallBrandDTO dto) {
        dto.setId(null);
        return Result.success(mallBrandService.save(dto));
    }

    @PutMapping
    @Operation(summary = "修改品牌")
    public Result<Long> update(@Valid @RequestBody MallBrandDTO dto) {
        return Result.success(mallBrandService.save(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除品牌（该品牌下有商品时拒绝）")
    public Result<Void> remove(@PathVariable Long id) {
        mallBrandService.remove(id);
        return Result.success();
    }
}
