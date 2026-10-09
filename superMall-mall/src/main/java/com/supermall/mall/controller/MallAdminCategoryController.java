package com.supermall.mall.controller;

import com.supermall.mall.dto.MallCategoryDTO;
import com.supermall.mall.service.MallCategoryService;
import com.supermall.mall.vo.MallCategoryTreeVO;
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
 * 运营后台 - 分类管理（多级）。认证：请求头 mall-admin-token。
 */
@RestController
@RequestMapping("/api/mall/admin/categories")
@Tag(name = "电商后台-分类")
@Slf4j
@RequiredArgsConstructor
public class MallAdminCategoryController {

    private final MallCategoryService mallCategoryService;

    @GetMapping("/tree")
    @Operation(summary = "分类树（含禁用的，后台编辑用）")
    public Result<List<MallCategoryTreeVO>> tree(@RequestParam(required = false) Integer status) {
        return Result.success(mallCategoryService.tree(status));
    }

    @PostMapping
    @Operation(summary = "新增分类（父分类层级 +1，最多 3 级）")
    public Result<Long> create(@Valid @RequestBody MallCategoryDTO dto) {
        dto.setId(null);
        return Result.success(mallCategoryService.save(dto));
    }

    @PutMapping
    @Operation(summary = "修改分类（不支持改父分类，避免子树层级错乱）")
    public Result<Long> update(@Valid @RequestBody MallCategoryDTO dto) {
        return Result.success(mallCategoryService.save(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类（有子分类或有商品时拒绝）")
    public Result<Void> remove(@PathVariable Long id) {
        mallCategoryService.remove(id);
        return Result.success();
    }
}
