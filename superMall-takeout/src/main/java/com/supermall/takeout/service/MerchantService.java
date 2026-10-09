package com.supermall.takeout.service;

import com.supermall.takeout.dto.MerchantApplyDTO;
import com.supermall.takeout.dto.MerchantLoginDTO;
import com.supermall.takeout.dto.MerchantUpdateDTO;
import com.supermall.takeout.entity.Merchant;
import com.supermall.takeout.vo.MerchantApplyVO;
import com.supermall.result.PageResult;
import com.supermall.takeout.vo.MerchantLoginVO;

import java.util.List;

public interface MerchantService {

    MerchantLoginVO login(MerchantLoginDTO dto);

    /**
     * 商家自助入驻申请：落库 audit_status=0 待审核、status=0 未营业。
     * 审核通过前不能登录。
     */
    MerchantApplyVO apply(MerchantApplyDTO dto);

    /** 查询某个邮箱的入驻申请状态（登录页「我的申请」用） */
    MerchantApplyVO applyStatus(String username);

    Merchant getById(Long id);

    void updateShop(MerchantUpdateDTO dto);

    /**
     * 用户端：营业中的商家列表（分页）。
     *
     * <p>原来返回全量数组，商家一多就是「一次全吐」；改成与其它列表一致的
     * {@link PageResult}（page 从 1 开始，pageSize 上限 100）。
     */
    PageResult listOpen(String keyword, Integer page, Integer pageSize);

    /** 用户端：商家详情 */
    Merchant getDetail(Long id);

    /** 商家自助营业状态（Redis 开关）：1营业中 0休息中，未设置默认营业中 */
    Integer getBusinessStatus(Long merchantId);

    /** 设置商家自助营业状态 */
    void updateBusinessStatus(Long merchantId, Integer businessStatus);
}
