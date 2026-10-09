package com.supermall.takeout.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SkuDTO implements Serializable {

    private Long id;

    private Long productId;

    @NotBlank(message = "规格名称不能为空")
    private String name;

    @NotNull(message = "规格价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "规格库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    private Integer status;
}
