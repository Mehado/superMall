package com.supermall.mall.service;

import com.supermall.result.PageResult;

/**
 * 浏览足迹（需登录）。
 *
 * <p>记录是「幂等去重」的：同一用户反复看同一个商品只保留一行，累加浏览次数。
 */
public interface MallFootprintService {

    /** 记录一次浏览（商品详情页调用） */
    void record(Long userId, Long productId);

    /** 足迹列表（按最近浏览倒序），含商品卡片 */
    PageResult page(Long userId, Integer page, Integer pageSize);

    /** 删除单条 */
    void removeOne(Long userId, Long productId);

    /** 清空 */
    void clear(Long userId);
}
