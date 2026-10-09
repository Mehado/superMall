package com.supermall.takeout.controller.merchant;

import com.supermall.takeout.annotations.NoRepeatSubmit;
import com.supermall.takeout.dto.ProductDTO;
import com.supermall.takeout.dto.ProductPageQueryDTO;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import com.supermall.takeout.service.ProductService;
import com.supermall.takeout.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant/products")
@Tag(name = "商家端-商品管理")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "商品分页查询")
    public Result<PageResult> page(@Valid ProductPageQueryDTO dto) {
        return Result.success(productService.pageQuery(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情（编辑回显）")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.success(productService.getForMerchant(id));
    }

    @NoRepeatSubmit
    @PostMapping
    @Operation(summary = "新增商品（可带规格）")
    public Result<Void> save(@Valid @RequestBody ProductDTO dto) {
        productService.save(dto);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改商品（可带规格）")
    public Result<Void> update(@Valid @RequestBody ProductDTO dto) {
        productService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品（仅下架可删）")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }
}
