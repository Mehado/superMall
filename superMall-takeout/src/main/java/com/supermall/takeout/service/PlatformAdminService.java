package com.supermall.takeout.service;

import com.supermall.takeout.dto.MerchantAuditDTO;
import com.supermall.takeout.dto.PlatformAdminLoginDTO;
import com.supermall.takeout.dto.PlatformAdminProfileDTO;
import com.supermall.takeout.entity.PlatformAdmin;
import com.supermall.takeout.vo.PlatformAdminLoginVO;
import com.supermall.takeout.vo.PlatformOverviewVO;
import com.supermall.result.PageResult;

/**
 * 平台运营（外卖）：商家入驻审核 + 骑手管理。
 */
public interface PlatformAdminService {

    PlatformAdminLoginVO login(PlatformAdminLoginDTO dto);

    /** 查自己的资料（password 置空） */
    PlatformAdmin getById(Long id);

    /** 改自己的资料（姓名） */
    void updateProfile(Long id, PlatformAdminProfileDTO dto);

    /** 商家入驻申请分页；auditStatus 不传 = 全部（待审核排前面） */
    PageResult pageMerchants(Integer auditStatus, String keyword, Integer page, Integer pageSize);

    /** 审核入驻申请：通过→营业；驳回→未营业并记录原因 */
    void auditMerchant(Long id, MerchantAuditDTO dto);

    /** 骑手分页 */
    PageResult pageDeliverymen(String keyword, Integer status, Integer page, Integer pageSize);

    /** 启用 / 停用骑手 */
    void updateDeliverymanStatus(Long id, Integer status);

    /** 平台看板 */
    PlatformOverviewVO overview();
}
