package com.supermall.takeout.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Data
public class ProductPageQueryDTO implements Serializable {

    @Min(value = 1, message = "页码不能小于1")
    private int page = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    private int pageSize = 10;

    /** 商家端查询时由服务端写入当前商家 id，用于数据隔离 */
    private Long merchantId;

    private String name;

    private Long categoryId;

    /** 业态筛选：1外卖食品 2百货商品 */
    private Integer businessType;

    private Integer status;
}
