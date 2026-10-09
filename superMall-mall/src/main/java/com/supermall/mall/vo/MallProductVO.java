package com.supermall.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品视图（列表 + 详情共用一个类）。
 *
 * <p>为什么不分两个类：详情只比列表多 4 个字段，拆成两个类要么继承（Lombok + 继承容易出坑），
 * 要么让前端维护两个类型。这里用**可空字段**表达：列表查询不查 {@code detail/images/specs/skus}
 * （SQL 里也不 select detail 这个 TEXT 字段），所以它们在列表接口里就是 null。
 *
 * <p>{@code priceLow / priceHigh} 构成价格区间（相等时前端只显示一个价）；
 * {@code stock} 是所有 SKU 库存之和 —— 两者都是服务端由 SKU 派生的冗余字段。
 *
 * <p>{@code specs / skus} 用**字符串**表示规格值，与 {@code MallProductSaveDTO} 完全对称：
 * 运营后台可以「GET 详情 → 改 → PUT 原样提交」，买家端按字符串匹配定位 SKU。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品（列表 / 详情）")
public class MallProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long categoryId;

    @Schema(description = "分类名")
    private String categoryName;

    private Long brandId;

    @Schema(description = "品牌名")
    private String brandName;

    private String name;

    private String subtitle;

    private String mainImage;

    private BigDecimal priceLow;

    private BigDecimal priceHigh;

    private Integer stock;

    private Integer sales;

    private BigDecimal rating;

    @Schema(description = "评价数（P5 派生）")
    private Integer reviewCount;

    @Schema(description = "好评率 %，4 星及以上占比（P5 派生）；无评价为 100.00")
    private BigDecimal goodRate;

    /** 1 上架 0 下架 */
    private Integer status;

    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /* ---------- 以下仅详情接口返回（列表为 null） ---------- */

    @Schema(description = "详情长文本；列表接口不返回")
    private String detail;

    @Schema(description = "多图，按顺序；列表接口不返回")
    private List<String> images;

    @Schema(description = "规格项；列表接口不返回")
    private List<MallSpecVO> specs;

    @Schema(description = "SKU 列表；列表接口不返回")
    private List<MallSkuVO> skus;
}
