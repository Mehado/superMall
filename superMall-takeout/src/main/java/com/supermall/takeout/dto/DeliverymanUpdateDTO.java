package com.supermall.takeout.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Data
public class DeliverymanUpdateDTO implements Serializable {

    private String name;

    @Pattern(regexp = "^$|^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    private String phone;
}
