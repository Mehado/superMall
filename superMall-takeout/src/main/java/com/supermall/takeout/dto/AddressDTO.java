package com.supermall.takeout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Data
public class AddressDTO implements Serializable {

    private Long id;

    @NotBlank(message = "收货人不能为空")
    private String consignee;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    private String phone;

    private String province;

    private String city;

    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detail;

    private String label;

    /** 1默认地址 0非默认 */
    private Integer isDefault;
}
