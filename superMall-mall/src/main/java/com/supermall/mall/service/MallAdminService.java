package com.supermall.mall.service;

import com.supermall.mall.dto.MallAdminLoginDTO;
import com.supermall.mall.entity.MallAdmin;
import com.supermall.mall.vo.MallAdminLoginVO;

/**
 * 运营后台管理员。
 */
public interface MallAdminService {

    /** 登录（需图形验证码，由 MallCaptchaAspect 在方法执行前校验） */
    MallAdminLoginVO login(MallAdminLoginDTO dto);

    /** 查管理员资料（password 置空） */
    MallAdmin getById(Long id);
}
