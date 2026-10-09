package com.supermall.mall.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.entity.MallFootprint;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallFootprintMapper;
import com.supermall.mall.mapper.MallProductMapper;
import com.supermall.mall.service.MallFootprintService;
import com.supermall.mall.vo.MallFootprintVO;
import com.supermall.mall.vo.MallProductVO;
import com.supermall.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MallFootprintServiceImpl implements MallFootprintService {

    private final MallFootprintMapper mallFootprintMapper;
    private final MallProductMapper mallProductMapper;

    @Override
    @Transactional
    public void record(Long userId, Long productId) {
        if (mallProductMapper.getById(productId) == null) {
            throw new MallBusinessException(MallMessageConstant.PRODUCT_NOT_FOUND);
        }
        mallFootprintMapper.upsert(userId, productId);
    }

    @Override
    public PageResult page(Long userId, Integer page, Integer pageSize) {
        int p = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        PageHelper.startPage(p, size);
        Page<MallFootprint> rows = mallFootprintMapper.pageByUser(userId);
        List<MallFootprint> footprints = rows.getResult();
        if (footprints.isEmpty()) {
            return new PageResult(rows.getTotal(), List.of());
        }

        // 一次批量取商品卡片，避免 for 里逐个查库（N+1）
        List<Long> productIds = footprints.stream().map(MallFootprint::getProductId).toList();
        Map<Long, MallProductVO> cardById = new HashMap<>();
        for (MallProductVO card : mallFootprintMapper.listProductCards(productIds)) {
            cardById.put(card.getId(), card);
        }

        List<MallFootprintVO> vos = new ArrayList<>(footprints.size());
        for (MallFootprint f : footprints) {
            vos.add(MallFootprintVO.builder()
                    .productId(f.getProductId())
                    .lastVisitTime(f.getUpdateTime())
                    .visitCount(f.getVisitCount())
                    .product(cardById.get(f.getProductId()))
                    .build());
        }
        return new PageResult(rows.getTotal(), vos);
    }

    @Override
    @Transactional
    public void removeOne(Long userId, Long productId) {
        mallFootprintMapper.deleteByUserAndProduct(userId, productId);
    }

    @Override
    @Transactional
    public void clear(Long userId) {
        mallFootprintMapper.deleteByUser(userId);
    }
}
