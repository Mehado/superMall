package com.supermall.mall.service;

import com.supermall.mall.vo.MallMemberVO;
import com.supermall.result.PageResult;

/**
 * 运营后台 - 会员管理。
 *
 * <p>会员就是买家（共享 {@code sys_user}）；统计其订单数与消费额。
 * 唯一写操作是启用/禁用，禁用后外卖与电商都无法登录（同一账号）。
 */
public interface MallMemberService {

    PageResult page(String keyword, Integer status, Integer page, Integer pageSize);

    MallMemberVO detail(Long id);

    /** 启用 / 禁用会员 */
    void updateStatus(Long id, Integer status);
}
