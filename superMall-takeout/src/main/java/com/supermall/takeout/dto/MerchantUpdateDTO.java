package com.supermall.takeout.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class MerchantUpdateDTO implements Serializable {

    @NotBlank(message = "店铺名称不能为空")
    private String shopName;

    private String logo;

    private String description;

    private String phone;

    private String address;

    /** 1营业 0停业 */
    private Integer status;
}
