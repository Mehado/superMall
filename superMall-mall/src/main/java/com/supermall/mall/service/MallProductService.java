package com.supermall.mall.service;

import com.supermall.mall.dto.MallProductPageQueryDTO;
import com.supermall.mall.dto.MallProductSaveDTO;
import com.supermall.mall.vo.MallProductVO;
import com.supermall.result.PageResult;

import java.util.List;

/**
 * 商品 SPU（含规格与 SKU）。
 */
public interface MallProductService {

    /**
     * 分页查询（后台与前台共用）：支持关键字搜索、分类/品牌/价格区间筛选、白名单排序。
     *
     * <p>传父分类会带上其后代分类的商品；前台接口会强制只看上架商品。
     */
    PageResult page(MallProductPageQueryDTO query);

    /**
     * 首页推荐商品（只返回上架商品）。
     *
     * @param type  hot=销量最高（默认）/ new=最新 / rating=评分最高
     * @param limit 条数，默认 10，上限 50
     */
    List<MallProductVO> recommend(String type, Integer limit);

    /** 详情：含多图、规格项、SKU 列表（直查 DB，后台编辑用，永远是最新的） */
    MallProductVO detail(Long id);

    /**
     * 公开目录用的详情：在 {@link #detail} 之上加 Redis 缓存（Cache-Aside）。
     *
     * <p>三防：查不到缓存空值（防穿透）、{@code setnx} 互斥锁 + 双重检查（防击穿）、
     * 随机 TTL（防雪崩）。商品有写操作时会主动删缓存。
     */
    MallProductVO detailCached(Long id);

    /** 新增/修改（SPU + 多图 + 规格 + SKU 一次提交）；返回商品 id */
    Long save(MallProductSaveDTO dto);

    /** 上下架：1 上架 0 下架 */
    void updateStatus(Long id, Integer status);

    /** 删除（连同多图/规格/SKU）；上架中的商品不允许删 */
    void remove(Long id);
}
