package com.supermall.takeout.mapper;

import com.supermall.takeout.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartItemMapper {

    void insert(CartItem item);

    void updateNumber(@Param("id") Long id, @Param("userId") Long userId, @Param("number") Integer number);

    void deleteById(@Param("id") Long id, @Param("userId") Long userId);

    void deleteByCartId(Long cartId);

    void deleteByUserId(Long userId);

    CartItem getById(Long id);

    CartItem getByProductAndSku(@Param("cartId") Long cartId,
                                @Param("productId") Long productId,
                                @Param("skuId") Long skuId);

    List<CartItem> listByCartId(Long cartId);

    List<CartItem> listByUserId(Long userId);

    List<CartItem> listByUserIdAndMerchant(@Param("userId") Long userId,
                                           @Param("merchantId") Long merchantId);
}
