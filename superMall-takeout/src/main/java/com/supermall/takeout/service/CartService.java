package com.supermall.takeout.service;

import com.supermall.takeout.dto.CartAddDTO;
import com.supermall.takeout.dto.CartUpdateDTO;
import com.supermall.takeout.entity.CartItem;
import com.supermall.takeout.vo.CartVO;

import java.util.List;

public interface CartService {

    void add(CartAddDTO dto);

    void updateNumber(CartUpdateDTO dto);

    void deleteItem(Long itemId);

    /** 清空某商家下的购物车 */
    void clearByMerchant(Long merchantId);

    /** 清空用户全部购物车 */
    void clearAll();

    /** 用户端：按商家分组的购物车 */
    List<CartVO> list();

    /** 下单用：某商家下的购物车明细 */
    List<CartItem> listByMerchant(Long merchantId);
}
