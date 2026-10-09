package com.supermall.takeout.controller.merchant;

import com.supermall.takeout.annotations.NoRepeatSubmit;
import com.supermall.takeout.dto.CategoryDTO;
import com.supermall.takeout.dto.CategoryPageQueryDTO;
import com.supermall.takeout.entity.Category;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import com.supermall.takeout.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant/categories")
@Tag(name = "商家端-分类管理")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "分类分页查询")
    public Result<PageResult> page(@Valid CategoryPageQueryDTO dto) {
        return Result.success(categoryService.pageQuery(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "分类详情")
    public Result<Category> detail(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @NoRepeatSubmit
    @PostMapping
    @Operation(summary = "新增分类")
    public Result<Void> save(@Valid @RequestBody CategoryDTO dto) {
        categoryService.save(dto);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改分类")
    public Result<Void> update(@Valid @RequestBody CategoryDTO dto) {
        categoryService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
