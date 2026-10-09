package com.supermall.takeout.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.AddressDTO;
import com.supermall.entity.Address;
import com.supermall.takeout.exception.AddressBusinessException;
import com.supermall.takeout.mapper.AddressMapper;
import com.supermall.takeout.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public void save(AddressDTO dto) {
        Long userId = BaseContext.getCurrentId();
        boolean first = addressMapper.countByUserId(userId) == 0;
        boolean asDefault = first || Integer.valueOf(1).equals(dto.getIsDefault());
        if (asDefault) {
            addressMapper.clearDefault(userId);
        }
        LocalDateTime now = LocalDateTime.now();
        Address address = new Address();
        BeanUtils.copyProperties(dto, address);
        address.setId(null);
        address.setUserId(userId);
        address.setIsDefault(asDefault ? 1 : 0);
        address.setCreateTime(now);
        address.setUpdateTime(now);
        addressMapper.insert(address);
    }

    @Override
    @Transactional
    public void update(AddressDTO dto) {
        Long userId = BaseContext.getCurrentId();
        Address exist = addressMapper.getById(dto.getId());
        if (exist == null || !userId.equals(exist.getUserId())) {
            throw new AddressBusinessException(MessageConstant.ADDRESS_NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(dto.getIsDefault())) {
            addressMapper.clearDefault(userId);
        }
        Address address = new Address();
        BeanUtils.copyProperties(dto, address);
        address.setUserId(userId);
        addressMapper.update(address);
    }

    @Override
    public void delete(Long id) {
        Address exist = addressMapper.getById(id);
        if (exist == null || !BaseContext.getCurrentId().equals(exist.getUserId())) {
            throw new AddressBusinessException(MessageConstant.ADDRESS_NOT_FOUND);
        }
        addressMapper.deleteById(id, BaseContext.getCurrentId());
    }

    @Override
    public List<Address> list() {
        return addressMapper.listByUserId(BaseContext.getCurrentId());
    }

    @Override
    public Address getById(Long id) {
        Address address = addressMapper.getById(id);
        if (address == null || !BaseContext.getCurrentId().equals(address.getUserId())) {
            throw new AddressBusinessException(MessageConstant.ADDRESS_NOT_FOUND);
        }
        return address;
    }

    @Override
    @Transactional
    public void setDefault(Long id) {
        Long userId = BaseContext.getCurrentId();
        Address exist = addressMapper.getById(id);
        if (exist == null || !userId.equals(exist.getUserId())) {
            throw new AddressBusinessException(MessageConstant.ADDRESS_NOT_FOUND);
        }
        addressMapper.clearDefault(userId);
        addressMapper.setDefault(id, userId);
    }
}
