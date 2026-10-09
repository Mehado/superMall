package com.supermall.takeout.controller.publicapi;

import com.supermall.takeout.dto.ProductQueryDTO;
import com.supermall.takeout.entity.Category;
import com.supermall.takeout.entity.Merchant;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import com.supermall.takeout.service.CategoryService;
import com.supermall.takeout.service.MerchantService;
import com.supermall.takeout.service.ProductService;
import com.supermall.takeout.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 无需登录的公共浏览接口（商家列表 / 商家详情 / 分类 / 商品）。
 */
@RestController
@RequestMapping("/api/public")
@Tag(name = "公共浏览接口")
@RequiredArgsConstructor
public class PublicController {

    private final MerchantService merchantService;
    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping("/merchants")
    @Operation(summary = "营业中的商家列表（分页）",
            description = "page 从 1 开始；pageSize 默认 20、上限 100；keyword 按店名模糊")
    public Result<PageResult> listMerchants(@RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(merchantService.listOpen(keyword, page, pageSize));
    }

    @GetMapping("/merchants/{id}")
    @Operation(summary = "商家详情")
    public Result<Merchant> merchantDetail(@PathVariable Long id) {
        return Result.success(merchantService.getDetail(id));
    }

    @GetMapping("/merchants/{id}/categories")
    @Operation(summary = "商家分类列表")
    public Result<List<Category>> categories(@PathVariable Long id) {
        return Result.success(categoryService.listByMerchant(id));
    }

    @GetMapping("/merchants/{id}/products")
    @Operation(summary = "商家商品分页浏览")
    public Result<PageResult> products(@PathVariable Long id, @Valid ProductQueryDTO dto) {
        dto.setMerchantId(id);
        return Result.success(productService.pageQueryForUser(dto));
    }

    @GetMapping("/products")
    @Operation(summary = "首页商品流：按业态随机抽取在售商品")
    public Result<List<ProductVO>> productFeed(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer businessType,
                                               @RequestParam(defaultValue = "6") int limit) {
        return Result.success(productService.randomFeed(keyword, businessType, limit));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "商品详情（带缓存）")
    public Result<ProductVO> productDetail(@PathVariable Long id) {
        return Result.success(productService.getDetail(id));
    }
}
