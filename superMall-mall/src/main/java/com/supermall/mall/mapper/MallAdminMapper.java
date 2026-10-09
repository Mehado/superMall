package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallAdmin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 运营后台管理员（mall_admin）。
 */
@Mapper
public interface MallAdminMapper {

    MallAdmin getByUsername(@Param("username") String username);

    MallAdmin getById(@Param("id") Long id);
}
