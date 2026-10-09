package com.supermall.mall.service.impl;

import com.supermall.entity.Address;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallAddressDTO;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallAddressMapper;
import com.supermall.mall.service.MallAddressService;
import com.supermall.mall.service.MallAreaService;
import com.supermall.mall.vo.MallAreaResolvedVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 收货地址实现。
 *
 * <p>三个约定：
 * <ol>
 *   <li><b>第一条地址自动默认</b>，之后手动设默认会先把其他地址的默认标记清掉
 *       （保证同一用户最多一个默认地址）。</li>
 *   <li><b>区划名称由服务端回填</b>：前端三级联动只传 areaCode，
 *       服务端校验它是末级节点后写入 province / city / district ——
 *       名称不会写错，也不会出现「code 是北京的、名称是上海的」这种脏数据。</li>
 *   <li><b>越权防护</b>：所有按 id 的操作都先比对 user_id。</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallAddressServiceImpl implements MallAddressService {

    private static final int MAX_ADDRESS = 20;
    private static final int DEFAULT_YES = 1;
    private static final int DEFAULT_NO = 0;

    private final MallAddressMapper mallAddressMapper;
    private final MallAreaService mallAreaService;

    @Override
    public List<Address> list(Long userId) {
        return mallAddressMapper.listByUserId(userId);
    }

    @Override
    public Address detail(Long userId, Long id) {
        return requireOwned(userId, id);
    }

    @Override
    @Transactional
    public Long save(Long userId, MallAddressDTO dto) {
        if (mallAddressMapper.countByUserId(userId) >= MAX_ADDRESS) {
            throw new MallBusinessException(MallMessageConstant.ADDRESS_LIMIT);
        }
        boolean first = mallAddressMapper.countByUserId(userId) == 0;
        boolean asDefault = first || Integer.valueOf(DEFAULT_YES).equals(dto.getIsDefault());
        if (asDefault) {
            mallAddressMapper.clearDefault(userId);
        }

        Address address = new Address();
        address.setUserId(userId);
        address.setConsignee(dto.getConsignee().trim());
        address.setPhone(dto.getPhone().trim());
        address.setDetail(dto.getDetail().trim());
        address.setLabel(dto.getLabel());
        applyArea(address, dto);
        address.setIsDefault(asDefault ? DEFAULT_YES : DEFAULT_NO);
        LocalDateTime now = LocalDateTime.now();
        address.setCreateTime(now);
        address.setUpdateTime(now);

        mallAddressMapper.insert(address);
        log.info("新增收货地址：userId={}, id={}, 默认={}", userId, address.getId(), address.getIsDefault());
        return address.getId();
    }

    @Override
    @Transactional
    public void update(Long userId, MallAddressDTO dto) {
        if (dto.getId() == null) {
            throw new MallBusinessException("修改地址必须传 id");
        }
        Address exist = requireOwned(userId, dto.getId());
        boolean asDefault = Integer.valueOf(DEFAULT_YES).equals(dto.getIsDefault());
        if (asDefault) {
            mallAddressMapper.clearDefault(userId);
        }

        Address address = new Address();
        address.setId(exist.getId());
        address.setUserId(userId);
        address.setConsignee(dto.getConsignee().trim());
        address.setPhone(dto.getPhone().trim());
        address.setDetail(dto.getDetail().trim());
        address.setLabel(dto.getLabel());
        applyArea(address, dto);
        // 已经是默认地址的，编辑时不该被顺手取消默认（除非显式传 0）
        if (asDefault) {
            address.setIsDefault(DEFAULT_YES);
        } else if (dto.getIsDefault() == null && Integer.valueOf(DEFAULT_YES).equals(exist.getIsDefault())) {
            address.setIsDefault(DEFAULT_YES);
        } else {
            address.setIsDefault(DEFAULT_NO);
        }
        address.setUpdateTime(LocalDateTime.now());

        mallAddressMapper.update(address);
        log.info("修改收货地址：userId={}, id={}", userId, exist.getId());
    }

    @Override
    @Transactional
    public void remove(Long userId, Long id) {
        // 幂等：删不存在的地址也返回成功
        mallAddressMapper.deleteById(id, userId);
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long id) {
        requireOwned(userId, id);
        mallAddressMapper.clearDefault(userId);
        mallAddressMapper.setDefault(id, userId);
    }

    @Override
    public Address requireOwned(Long userId, Long id) {
        Address address = id == null ? null : mallAddressMapper.getById(id);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new MallBusinessException(MallMessageConstant.ADDRESS_NOT_FOUND);
        }
        return address;
    }

    /**
     * 省市区：优先按 areaCode 反查名称；没传 code 则要求三个名称齐全（兼容手工录入）。
     */
    private void applyArea(Address address, MallAddressDTO dto) {
        if (hasText(dto.getAreaCode())) {
            MallAreaResolvedVO resolved = mallAreaService.resolve(dto.getAreaCode());
            address.setAreaCode(resolved.getAreaCode());
            address.setProvince(resolved.getProvince());
            address.setCity(resolved.getCity());
            address.setDistrict(resolved.getDistrict());
            return;
        }
        if (!hasText(dto.getProvince()) || !hasText(dto.getCity()) || !hasText(dto.getDistrict())) {
            throw new MallBusinessException(MallMessageConstant.ADDRESS_AREA_REQUIRED);
        }
        address.setAreaCode(null);
        address.setProvince(dto.getProvince().trim());
        address.setCity(dto.getCity().trim());
        address.setDistrict(dto.getDistrict().trim());
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
