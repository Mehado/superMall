package com.supermall.takeout.vo;

import com.supermall.takeout.entity.OrderItem;
import com.supermall.takeout.entity.Orders;
import com.supermall.takeout.enumeration.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO extends Orders implements Serializable {

    /** 店铺名称 */
    private String shopName;

    /** 订单状态中文描述 */
    private String statusDesc;

    /** 配送单状态 */
    private String deliveryStatus;

    /** 配送员姓名 */
    private String deliverymanName;

    private String deliverymanPhone;

    /** 订单明细 */
    private List<OrderItem> orderItems;

    public String getStatusDesc() {
        if (statusDesc != null) {
            return statusDesc;
        }
        try {
            return OrderStatus.valueOf(getStatus()).getDesc();
        } catch (Exception e) {
            return getStatus();
        }
    }
}
