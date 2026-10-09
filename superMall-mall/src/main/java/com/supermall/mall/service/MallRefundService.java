package com.supermall.mall.service;

import com.supermall.mall.dto.MallRefundApplyDTO;
import com.supermall.mall.dto.MallRefundAuditDTO;
import com.supermall.mall.vo.MallRefundVO;
import com.supermall.result.PageResult;

/**
 * 退货退款（买家申请 + 运营后台审核）。
 *
 * <p>电商是自营，没有商家，「商家审核」= 运营后台审核。
 * 整单退款：申请时订单变 REFUNDING，审核通过变 REFUNDED 并回补库存 + 扣回销量；
 * 驳回则回到申请前的状态。
 */
public interface MallRefundService {

    /** 买家申请退款（一单同时只能有一个进行中的申请） */
    MallRefundVO apply(Long userId, String orderNumber, MallRefundApplyDTO dto);

    /** 买家查看某订单的退款进度 */
    MallRefundVO detailForUser(Long userId, String orderNumber);

    /* ---------------- 运营后台 ---------------- */

    PageResult pageForAdmin(String status, Integer page, Integer pageSize);

    MallRefundVO detailForAdmin(Long id);

    /** 审核：同意则回补库存、扣回销量；驳回则订单状态回退 */
    void audit(Long id, MallRefundAuditDTO dto);
}
