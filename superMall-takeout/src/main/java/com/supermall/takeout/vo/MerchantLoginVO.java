package com.supermall.takeout.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantLoginVO implements Serializable {

    private Long id;

    private String username;

    private String shopName;

    private String token;
}
