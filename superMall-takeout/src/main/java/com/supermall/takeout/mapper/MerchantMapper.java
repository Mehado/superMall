package com.supermall.takeout.mapper;

import com.supermall.takeout.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MerchantMapper {

    void insert(Merchant merchant);

    /**
     * 入驻申请：新增商家。
     * 相对 {@link #insert} 多了联系人 / 执照 / 审核字段，且落库时 audit_status=0、status=0。
     */
    void insertApply(Merchant merchant);

    Merchant getByUsername(String username);

    Merchant getById(Long id);

    void update(Merchant merchant);

    /** 用户端浏览：营业中的商家列表 */
    /** 平台营业中的商家（用户端列表；支持店名模糊） */
    List<Merchant> listOpen(@Param("keyword") String keyword);

    /** 取商家自助营业状态（DB 为准） */
    Integer getBusinessStatusById(@Param("id") Long id);

    /** 更新商家自助营业状态 */
    int updateBusinessStatus(@Param("id") Long id, @Param("businessStatus") Integer businessStatus);

    /* ---------- 平台运营：入驻审核 ---------- */

    /** 被驳回后重新提交入驻申请：复用同一行，条件更新回「待审核」 */
    int reapply(Merchant merchant);

    /** 按审核状态 / 关键字（店铺名 / 邮箱 / 手机号）分页，PageHelper 生效 */
    List<Merchant> pageByAuditStatus(@Param("auditStatus") Integer auditStatus,
                                     @Param("keyword") String keyword);

    /**
     * 审核（条件更新）：只有「待审核」的申请能被审。
     * 并发或重复审核只会成功一次，天然幂等。
     */
    int audit(@Param("id") Long id,
              @Param("auditStatus") Integer auditStatus,
              @Param("status") Integer status,
              @Param("rejectReason") String rejectReason,
              @Param("auditTime") LocalDateTime auditTime);

    /** 某审核状态下的商家数（平台看板用） */
    int countByAuditStatus(@Param("auditStatus") Integer auditStatus);
}
