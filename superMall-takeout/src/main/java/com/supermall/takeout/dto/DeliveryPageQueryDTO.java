package com.supermall.takeout.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Data
public class DeliveryPageQueryDTO implements Serializable {

    @Min(value = 1, message = "页码不能小于1")
    private int page = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    private int pageSize = 10;

    /** 配送单状态 */
    private String status;
}
