package com.supermall.mall.service;

import com.supermall.entity.Address;
import com.supermall.mall.dto.MallAddressDTO;

import java.util.List;

/**
 * 收货地址（共享 {@code address} 表）。
 *
 * <p>与外卖的地址管理是「同一张表、两套接口」：
 * 电商这套多了 {@code areaCode}（省市区末级编码）的校验与名称回填，
 * 且所有读写都限定在「当前登录用户自己的地址」。
 */
public interface MallAddressService {

    /** 地址列表（默认地址排最前） */
    List<Address> list(Long userId);

    /** 地址详情（含归属校验） */
    Address detail(Long userId, Long id);

    /** 新增地址，返回 id；第一条地址自动成为默认地址 */
    Long save(Long userId, MallAddressDTO dto);

    /** 修改地址（仅本人） */
    void update(Long userId, MallAddressDTO dto);

    /** 删除地址（幂等） */
    void remove(Long userId, Long id);

    /** 设为默认地址（仅本人） */
    void setDefault(Long userId, Long id);

    /** 取一条属于该用户的地址，否则抛业务异常（下单时会复用） */
    Address requireOwned(Long userId, Long id);
}
