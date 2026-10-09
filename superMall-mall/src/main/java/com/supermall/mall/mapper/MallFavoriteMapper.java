package com.supermall.mall.mapper;

import com.supermall.mall.entity.MallFavorite;
import com.supermall.mall.vo.MallProductVO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品收藏（mall_favorite）。
 */
@Mapper
public interface MallFavoriteMapper {

    /**
     * 收藏（幂等）：唯一键冲突时不报错。
     * 用 INSERT IGNORE 而不是先查后插 —— 后者在并发下会撞唯一键。
     */
    int insertIgnore(@Param("userId") Long userId, @Param("productId") Long productId);

    /** 取消收藏；返回影响行数（0 表示本来就没收藏） */
    int delete(@Param("userId") Long userId, @Param("productId") Long productId);

    /** 是否已收藏 */
    int countByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    /** 收藏总数 */
    int countByUser(@Param("userId") Long userId);

    /** 收藏列表（商品卡片，按收藏时间倒序） */
    Page<MallProductVO> pageProducts(@Param("userId") Long userId);
}
