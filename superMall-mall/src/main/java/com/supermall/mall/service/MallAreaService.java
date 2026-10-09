package com.supermall.mall.service;

import com.supermall.mall.vo.MallAreaResolvedVO;
import com.supermall.mall.vo.MallAreaVO;

import java.util.List;

/**
 * 行政区划（省 / 市 / 区县），支撑收货地址的「省市区三级联动」。
 *
 * <p>数据源是 {@code resources/mall-area.json}（3800+ 条静态区划），
 * 启动时一次性加载进内存：区划数据几乎不变，没有必要为它建表、走数据库查询。
 * 前端逐级查询：{@code parentCode=86} → 省，再拿省 code → 市，再拿市 code → 区县。
 */
public interface MallAreaService {

    /** 根节点编码（全国） */
    String ROOT_CODE = "86";

    /**
     * 查询下级区划。{@code parentCode} 为空时返回全部省份。
     *
     * <p>未知 parentCode 返回空列表（前端传错不会 500）。
     */
    List<MallAreaVO> children(String parentCode);

    /**
     * 按末级编码反查省 / 市 / 区名称。
     *
     * @throws com.supermall.mall.exception.MallBusinessException 编码不存在，或不是末级节点
     */
    MallAreaResolvedVO resolve(String areaCode);

    /** 编码是否存在 */
    boolean exists(String code);
}
