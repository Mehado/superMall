package com.supermall.mall.mapper;

import com.github.pagehelper.Page;
import com.supermall.mall.vo.MallMemberVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 会员管理（运营后台）—— 读共享表 {@code sys_user}，并统计其下单/评价情况。
 *
 * <p>与 {@link MallUserMapper}（买家侧）分开：
 * 买家侧坚持「只查/只插」，这里多了「按条件分页」与「改启用状态」，
 * 隔离成独立 Mapper 后，买家侧的约定不会被破坏。
 *
 * <p>⚠️ 唯一的写操作是改 {@code status}（启用/禁用），不碰密码等其它字段；
 * 禁用后外卖与电商都无法登录（同一账号）。
 */
@Mapper
public interface MallMemberMapper {

    Page<MallMemberVO> page(@Param("keyword") String keyword, @Param("status") Integer status);

    MallMemberVO getById(@Param("id") Long id);

    int countById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
