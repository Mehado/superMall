package com.supermall.takeout.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户端浏览商品。
 * <p>
 * 注意：merchantId 由 Controller 从路径参数写入，属于服务端填充字段，
 * 因此不能加 @NotNull —— @Valid 会在方法体执行之前校验，会导致永远校验失败。
 */
@Data
public class ProductQueryDTO implements Serializable {

    /** 服务端从 /api/public/merchants/{id}/products 的路径写入 */
    private Long merchantId;

    @Min(value = 1, message = "页码不能小于1")
    private int page = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    private int pageSize = 20;

    private Long categoryId;

    /** 业态筛选：1外卖食品 2百货商品 */
    private Integer businessType;

    private String keyword;
}
