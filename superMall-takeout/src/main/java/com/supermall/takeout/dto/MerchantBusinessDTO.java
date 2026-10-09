package com.supermall.takeout.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 商家自助营业状态（Redis 开关）
 */
@Data
public class MerchantBusinessDTO implements Serializable {

    /** 1营业中 0休息中 */
    @NotNull(message = "营业状态不能为空")
    private Integer businessStatus;
}
