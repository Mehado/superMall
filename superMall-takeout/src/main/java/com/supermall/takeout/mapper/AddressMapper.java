package com.supermall.takeout.mapper;

import com.supermall.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AddressMapper {

    void insert(Address address);

    void update(Address address);

    void deleteById(@Param("id") Long id, @Param("userId") Long userId);

    Address getById(@Param("id") Long id);

    List<Address> listByUserId(Long userId);

    /** 把该用户所有地址置为非默认 */
    void clearDefault(@Param("userId") Long userId);

    /** 设置指定地址为默认 */
    void setDefault(@Param("id") Long id, @Param("userId") Long userId);

    int countByUserId(Long userId);
}
