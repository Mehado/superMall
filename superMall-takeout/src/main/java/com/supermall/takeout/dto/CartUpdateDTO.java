package com.supermall.takeout.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class CartUpdateDTO implements Serializable {

    @NotNull(message = "购物车明细id不能为空")
    private Long id;

    @NotNull(message = "数量不能为空")
    @Min(value = 0, message = "数量不能为负数")
    private Integer number;
}
