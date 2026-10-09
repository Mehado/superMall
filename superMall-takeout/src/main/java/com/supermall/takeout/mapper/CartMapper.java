package com.supermall.takeout.mapper;

import com.supermall.takeout.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CartMapper {

    void insert(Cart cart);

    Cart getByUserAndMerchant(@Param("userId") Long userId, @Param("merchantId") Long merchantId);

    void deleteById(Long id);
}
