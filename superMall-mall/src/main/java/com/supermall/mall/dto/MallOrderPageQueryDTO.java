package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单分页查询（买家 / 运营后台共用）。
 */
@Data
@Schema(description = "订单分页查询")
public class MallOrderPageQueryDTO implements Serializable {

    @Schema(description = "订单状态：PENDING_PAYMENT / PENDING_DELIVERY / PENDING_RECEIPT / COMPLETED / CANCELLED / REFUNDING / REFUNDED")
    private String status;

    @Schema(description = "订单号（精确匹配，后台用）")
    private String orderNumber;

    @Schema(description = "买家 id（后台用；买家端忽略此参数，强制只看自己的）")
    private Long userId;

    private Integer page;

    private Integer pageSize;

    public int safePage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int safePageSize() {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }
}
