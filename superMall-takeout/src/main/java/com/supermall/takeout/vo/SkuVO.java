package com.supermall.takeout.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuVO implements Serializable {

    private Long id;

    private Long productId;

    private String name;

    private BigDecimal price;

    private Integer stock;

    private Integer status;
}
