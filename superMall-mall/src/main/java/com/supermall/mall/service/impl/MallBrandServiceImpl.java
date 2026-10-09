package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallBrandDTO;
import com.supermall.mall.entity.MallBrand;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallBrandMapper;
import com.supermall.mall.service.MallBrandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MallBrandServiceImpl implements MallBrandService {

    private static final int ENABLED = 1;

    private final MallBrandMapper mallBrandMapper;

    @Override
    public List<MallBrand> list(String name, Integer status) {
        return mallBrandMapper.list(name, status);
    }

    @Override
    public List<MallBrand> listEnabled() {
        return mallBrandMapper.listEnabled();
    }

    @Override
    public MallBrand getById(Long id) {
        MallBrand brand = mallBrandMapper.getById(id);
        if (brand == null) {
            throw new MallBusinessException(MallMessageConstant.BRAND_NOT_FOUND);
        }
        return brand;
    }

    @Override
    @Transactional
    public Long save(MallBrandDTO dto) {
        // 同名校验（排除自己）
        MallBrand same = mallBrandMapper.getByName(dto.getName());
        if (same != null && !same.getId().equals(dto.getId())) {
            throw new MallBusinessException(MallMessageConstant.BRAND_NAME_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        if (dto.getId() == null) {
            MallBrand brand = MallBrand.builder()
                    .name(dto.getName())
                    .logo(dto.getLogo())
                    .description(dto.getDescription())
                    .sort(dto.getSort() == null ? 0 : dto.getSort())
                    .status(dto.getStatus() == null ? ENABLED : dto.getStatus())
                    .createTime(now)
                    .updateTime(now)
                    .build();
            mallBrandMapper.insert(brand);
            return brand.getId();
        }

        getById(dto.getId());
        mallBrandMapper.update(MallBrand.builder()
                .id(dto.getId())
                .name(dto.getName())
                .logo(dto.getLogo())
                .description(dto.getDescription())
                .sort(dto.getSort())
                .status(dto.getStatus())
                .updateTime(now)
                .build());
        return dto.getId();
    }

    @Override
    @Transactional
    public void remove(Long id) {
        getById(id);
        // 有商品引用就不能删，否则商品会指向一个不存在的品牌
        if (mallBrandMapper.countProducts(id) > 0) {
            throw new MallBusinessException(MallMessageConstant.BRAND_IN_USE);
        }
        mallBrandMapper.deleteById(id);
    }
}
