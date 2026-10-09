package com.supermall.takeout.mapper;

import com.supermall.takeout.entity.PlatformAdmin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 平台运营账号（platform_admin）。
 */
@Mapper
public interface PlatformAdminMapper {

    PlatformAdmin getByUsername(String username);

    PlatformAdmin getById(Long id);

    /** 改自己的资料（目前只有 name；只更新非 null 字段） */
    int update(PlatformAdmin admin);
}
