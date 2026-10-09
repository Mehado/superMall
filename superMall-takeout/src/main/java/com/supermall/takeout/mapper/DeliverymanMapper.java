package com.supermall.takeout.mapper;

import com.supermall.takeout.entity.Deliveryman;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeliverymanMapper {

    void insert(Deliveryman deliveryman);

    Deliveryman getByUsername(String username);

    Deliveryman getById(Long id);

    void update(Deliveryman deliveryman);

    /* ---------- 平台运营：骑手管理 ---------- */

    /** 骑手列表（关键字匹配姓名 / 邮箱 / 手机号），PageHelper 生效 */
    List<Deliveryman> pageByCondition(@Param("keyword") String keyword,
                                      @Param("status") Integer status);

    /** 启用 / 停用骑手 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 可接单骑手数（平台看板用） */
    int countByStatus(@Param("status") Integer status);
}
