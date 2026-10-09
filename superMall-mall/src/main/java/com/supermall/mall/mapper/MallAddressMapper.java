package com.supermall.mall.mapper;

import com.supermall.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收货地址（共享的 {@code address} 表）。
 *
 * <p>电商与外卖共用这张表：外卖侧不读写 {@code area_code}，
 * 电商侧读写它并把区划名称冗余进 province / city / district。
 * 两边只在「同一个用户自己的地址」范围内操作，互不干扰。
 */
@Mapper
public interface MallAddressMapper {

    Address getById(@Param("id") Long id);

    /** 默认地址排最前，其余按最近修改倒序 */
    List<Address> listByUserId(@Param("userId") Long userId);

    int countByUserId(@Param("userId") Long userId);

    int insert(Address address);

    int update(Address address);

    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    /** 把该用户所有地址的默认标记清 0 */
    int clearDefault(@Param("userId") Long userId);

    /** 设为默认（仅本人） */
    int setDefault(@Param("id") Long id, @Param("userId") Long userId);
}
