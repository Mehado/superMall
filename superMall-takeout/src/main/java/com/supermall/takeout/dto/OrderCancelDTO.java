package com.supermall.takeout.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class OrderCancelDTO implements Serializable {

    @NotNull(message = "订单id不能为空")
    private Long id;

    private String cancelReason;
}
