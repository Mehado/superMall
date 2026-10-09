package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品分页查询（**后台与前台共用**）。
 *
 * <p>前台的搜索/筛选/排序就是这套参数；前台接口会强制把 {@code status} 覆盖成 1，
 * 所以前端传 status 也没用（不会泄露下架商品）。
 */
@Data
@Schema(description = "商品分页查询")
public class MallProductPageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码，默认 1")
    private Integer page = 1;

    @Schema(description = "每页条数，默认 10，上限 100")
    private Integer pageSize = 10;

    @Schema(description = "关键字：模糊匹配 商品名 / 副标题 / 品牌名 / 分类名")
    private String keyword;

    @Schema(description = "商品名模糊匹配（后台用；前台请用 keyword）")
    private String name;

    @Schema(description = "分类 id（传父分类会带上所有后代分类的商品）")
    private Long categoryId;

    private Long brandId;

    @Schema(description = "价格区间下限（含）；与 SKU 价格区间有交集即命中")
    private BigDecimal minPrice;

    @Schema(description = "价格区间上限（含）")
    private BigDecimal maxPrice;

    @Schema(description = """
            排序，可选值：
            default（默认：sort 升序 + id 倒序）
            price_asc / price_desc（按最低价）
            sales_desc（销量）
            rating_desc（评分）
            newest（最新）
            非法值会被忽略并回落到 default。
            """)
    private String orderBy;

    @Schema(description = "1 上架 0 下架；不传=全部。**前台接口会强制为 1**")
    private Integer status;

    /**
     * 服务端内部使用，前端不要传。
     *
     * <p>传了 categoryId 时，服务端会把它连同所有后代分类展开到这里
     * （这样传父分类「服饰鞋包」能查到 T恤/衬衫/运动鞋 下的商品）。
     */
    @Schema(hidden = true)
    private List<Long> categoryIds;

    public int safePage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    /** 归一化排序：只认白名单，其它一律回落 default（防止把脏值带进 SQL 分支判断） */
    public String safeOrderBy() {
        if (orderBy == null) {
            return "default";
        }
        return switch (orderBy) {
            case "price_asc", "price_desc", "sales_desc", "rating_desc", "newest" -> orderBy;
            default -> "default";
        };
    }
}
