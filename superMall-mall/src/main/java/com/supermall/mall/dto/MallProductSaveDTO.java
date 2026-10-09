package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 SPU + 规格 + SKU 一起提交（新增/修改共用）。
 *
 * <p>【为什么 SKU 用「规格值字符串」而不是 id 引用规格值】
 * 新增商品时规格项/规格值/ SKU 是同一次请求里一起创建的，前端此时拿不到库里的 id。
 * 所以约定：
 * <pre>
 *   specs = [ {name:"颜色", values:["白","黑"]},
 *             {name:"尺码", values:["M","L"]} ]
 *   skus  = [ {price:79, stock:100, specValues:["白","M"]},
 *             {price:79, stock:120, specValues:["白","L"]}, ... ]
 * </pre>
 * {@code specValues} 按 {@code specs} 的顺序一一对应，服务端负责解析成 id
 * 并强制校验「组合数与笛卡尔积一致」（见 MallProductServiceImpl#validateSkuMatrix）。
 *
 * <p>无规格商品：{@code specs} 留空、{@code skus} 只给 1 条且 {@code specValues} 为空。
 * —— 不做"无规格"特例，一律落到 SKU，购物车/下单/库存只有一套逻辑。
 */
@Data
@Schema(description = "商品保存（SPU + 规格 + SKU）")
public class MallProductSaveDTO implements Serializable {

    @Schema(description = "不传=新增，传=修改")
    private Long id;

    @NotNull(message = "请选择分类")
    @Schema(description = "叶子分类 id")
    private Long categoryId;

    @Schema(description = "品牌 id，可空")
    private Long brandId;

    @NotBlank(message = "商品名不能为空")
    @Size(max = 100, message = "商品名最长 100 个字符")
    private String name;

    @Size(max = 200, message = "卖点副标题最长 200 个字符")
    private String subtitle;

    @Schema(description = "主图 URL；不传则取 images 的第一张")
    private String mainImage;

    private String detail;

    private Integer sort;

    @Schema(description = "1 上架 0 下架；不传默认 0（先配好 SKU 再上架）")
    private Integer status;

    @Schema(description = "多图，最多 10 张，按顺序")
    @Size(max = 10, message = "商品图片最多 10 张")
    private List<String> images;

    @Valid
    @Schema(description = "规格项，最多 3 个；无规格商品留空")
    private List<SpecDTO> specs;

    @Valid
    @Schema(description = "SKU 列表，至少 1 条")
    private List<SkuDTO> skus;

    @Data
    @Schema(description = "规格项")
    public static class SpecDTO implements Serializable {

        @NotBlank(message = "规格名不能为空")
        @Size(max = 30, message = "规格名最长 30 个字符")
        private String name;

        @Schema(description = "规格值，至少 1 个，按顺序")
        private List<String> values;
    }

    @Data
    @Schema(description = "SKU")
    public static class SkuDTO implements Serializable {

        @Schema(description = "修改时可带上已有 SKU 的 id")
        private Long id;

        @Size(max = 64, message = "商家编码最长 64 个字符")
        private String skuCode;

        @NotNull(message = "SKU 价格不能为空")
        @DecimalMin(value = "0.01", message = "SKU 价格必须大于 0")
        private BigDecimal price;

        @NotNull(message = "SKU 库存不能为空")
        @Min(value = 0, message = "SKU 库存不能为负")
        private Integer stock;

        private String image;

        @Schema(description = "1 启用 0 禁用；不传默认 1")
        private Integer status;

        @Schema(description = "规格值，按 specs 顺序对应；无规格商品传空数组")
        private List<String> specValues;
    }
}
