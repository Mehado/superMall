package com.supermall.mall.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallMemberMapper;
import com.supermall.mall.service.MallMemberService;
import com.supermall.mall.vo.MallMemberVO;
import com.supermall.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会员管理实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallMemberServiceImpl implements MallMemberService {

    private static final int ENABLE = 1;
    private static final int DISABLE = 0;

    private final MallMemberMapper mallMemberMapper;

    @Override
    public PageResult page(String keyword, Integer status, Integer page, Integer pageSize) {
        int p = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        PageHelper.startPage(p, size);
        Page<MallMemberVO> result = mallMemberMapper.page(trimToNull(keyword), status);
        return new PageResult(result.getTotal(), result.getResult());
    }

    @Override
    public MallMemberVO detail(Long id) {
        MallMemberVO vo = id == null ? null : mallMemberMapper.getById(id);
        if (vo == null) {
            throw new MallBusinessException(MallMessageConstant.MEMBER_NOT_FOUND);
        }
        return vo;
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != ENABLE && status != DISABLE)) {
            throw new MallBusinessException(MallMessageConstant.MEMBER_STATUS_INVALID);
        }
        if (mallMemberMapper.countById(id) == 0) {
            throw new MallBusinessException(MallMessageConstant.MEMBER_NOT_FOUND);
        }
        mallMemberMapper.updateStatus(id, status);
        log.info("会员状态变更：id={}, status={}", id, status);
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
