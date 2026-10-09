package com.supermall.takeout.vo;

import com.supermall.takeout.entity.DeliveryOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrderVO extends DeliveryOrder implements Serializable {

    private String orderNumber;

    private String shopName;

    private String merchantPhone;

    /** 取餐地址 = 商家地址 */
    private String pickupAddress;

    /** 收货地址 */
    private String receiverAddress;

    private String consignee;

    private String phone;

    /** 订单金额，配送员可见配送费 */
    private java.math.BigDecimal deliveryFee;
}
