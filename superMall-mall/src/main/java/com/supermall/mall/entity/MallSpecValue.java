package com.supermall.mall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 规格值（mall_spec_value）—— 如「白」「黑」「M」「L」。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallSpecValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long specId;

    /** 规格值 */
    private String value;

    private Integer sort;

    private LocalDateTime createTime;
}
