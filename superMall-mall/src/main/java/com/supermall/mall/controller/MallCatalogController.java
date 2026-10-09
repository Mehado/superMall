package com.supermall.mall.controller;

import com.supermall.mall.dto.MallProductPageQueryDTO;
import com.supermall.mall.entity.MallBanner;
import com.supermall.mall.entity.MallBrand;
import com.supermall.mall.service.MallAreaService;
import com.supermall.mall.service.MallBannerService;
import com.supermall.mall.service.MallBrandService;
import com.supermall.mall.service.MallCategoryService;
import com.supermall.mall.service.MallProductService;
import com.supermall.mall.service.MallReviewService;
import com.supermall.mall.vo.MallAreaVO;
import com.supermall.mall.vo.MallCategoryTreeVO;
import com.supermall.mall.vo.MallProductVO;
import com.supermall.mall.vo.MallReviewStatsVO;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品目录（买家侧，无需登录）。
 *
 * <p>路径 {@code /api/mall/catalog/**} 不在任何拦截器的 addPathPatterns 里，所以天然公开 ——
 * 与外卖的 {@code /api/public/**} 同理：游客先浏览，加购/下单才要登录。
 */
@RestController
@RequestMapping("/api/mall/catalog")
@Tag(name = "电商-商品目录（公开）")
@Slf4j
@RequiredArgsConstructor
public class MallCatalogController {

    private static final int ON_SALE = 1;

    private final MallCategoryService mallCategoryService;
    private final MallBrandService mallBrandService;
    private final MallProductService mallProductService;
    private final MallBannerService mallBannerService;
    private final MallAreaService mallAreaService;
    private final MallReviewService mallReviewService;

    /* ---------------- 首页 ---------------- */

    @GetMapping("/banners")
    @Operation(summary = "推荐位（只返回已上线且在生效期内的）",
            description = "position：1 首页轮播 2 首页金刚区 3 分类页顶部；不传=全部位置")
    public Result<List<MallBanner>> banners(@RequestParam(required = false) Integer position) {
        return Result.success(mallBannerService.listEffective(position));
    }

    @GetMapping("/recommend")
    @Operation(summary = "推荐商品（只返回上架商品）",
            description = "type：hot=销量最高（默认）/ new=最新 / rating=评分最高；limit 默认 10，上限 50")
    public Result<List<MallProductVO>> recommend(@RequestParam(required = false) String type,
                                                 @RequestParam(required = false) Integer limit) {
        return Result.success(mallProductService.recommend(type, limit));
    }

    /* ---------------- 分类导航 ---------------- */

    @GetMapping("/categories")
    @Operation(summary = "分类树（只含启用的，含在售商品数）",
            description = "productCount 是「含后代分类」的在售商品总数，分类导航显示「手机(23)」用")
    public Result<List<MallCategoryTreeVO>> categories() {
        return Result.success(mallCategoryService.treeWithProductCount());
    }

    @GetMapping("/brands")
    @Operation(summary = "品牌列表（只含启用的）")
    public Result<List<MallBrand>> brands() {
        return Result.success(mallBrandService.listEnabled());
    }

    /* ---------------- 行政区划（收货地址三级联动） ---------------- */

    @GetMapping("/areas")
    @Operation(summary = "省市区三级联动（无需登录）",
            description = """
                    传 parentCode 逐级查询：
                    - 不传或 86   → 省份列表
                    - 传省 code   → 该省的市列表
                    - 传市 code   → 该市的区县列表
                    返回项的 hasChildren=false 表示可以选作地址末级。
                    """)
    public Result<List<MallAreaVO>> areas(@RequestParam(required = false) String parentCode) {
        return Result.success(mallAreaService.children(parentCode));
    }

    /* ---------------- 搜索与筛选 ---------------- */

    @GetMapping("/products/page")
    @Operation(summary = "商品搜索 / 筛选 / 排序（只返回上架商品）",
            description = """
                    参数（都可选）：
                    - keyword      关键字，模糊匹配 商品名 / 副标题 / 品牌名 / 分类名
                    - categoryId   分类（传父分类会自动带上所有后代分类的商品）
                    - brandId      品牌
                    - minPrice / maxPrice  价格区间（与商品价格区间有交集即命中）
                    - orderBy      default | price_asc | price_desc | sales_desc | rating_desc | newest
                    - page / pageSize      pageSize 默认 10，上限 100

                    传了 status 也会被服务端覆盖成 1（前台只能看到上架商品）。
                    """)
    public Result<PageResult> products(MallProductPageQueryDTO query) {
        // 强制只看上架：防止把后台参数带出来
        query.setStatus(ON_SALE);
        return Result.success(mallProductService.page(query));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "商品详情（含多图 / 规格项 / SKU 列表，带 Redis 缓存）")
    public Result<MallProductVO> product(@PathVariable Long id) {
        return Result.success(mallProductService.detailCached(id));
    }

    /* ---------------- 商品评价（公开） ---------------- */

    @GetMapping("/products/{productId}/reviews")
    @Operation(summary = "商品评价分页（无需登录）",
            description = "可按 rating（1-5）与 hasImage=true 过滤；匿名评价的 nickname 固定为「匿名用户」")
    public Result<PageResult> productReviews(@PathVariable Long productId,
                                             @RequestParam(required = false) Integer rating,
                                             @RequestParam(required = false) Boolean hasImage,
                                             @RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(mallReviewService.pageByProduct(productId, rating, hasImage, page, pageSize));
    }

    @GetMapping("/products/{productId}/review-stats")
    @Operation(summary = "商品评价统计（评分 / 评价数 / 好评率 / 星级分布，无需登录）")
    public Result<MallReviewStatsVO> productReviewStats(@PathVariable Long productId) {
        return Result.success(mallReviewService.stats(productId));
    }
}
