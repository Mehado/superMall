package com.supermall.takeout.service;

import com.supermall.takeout.dto.AddressDTO;
import com.supermall.entity.Address;

import java.util.List;

public interface AddressService {

    void save(AddressDTO dto);

    void update(AddressDTO dto);

    void delete(Long id);

    List<Address> list();

    Address getById(Long id);

    void setDefault(Long id);
}
