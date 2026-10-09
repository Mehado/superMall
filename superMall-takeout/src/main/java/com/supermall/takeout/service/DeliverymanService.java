package com.supermall.takeout.service;

import com.supermall.takeout.dto.DeliverymanLoginDTO;
import com.supermall.takeout.dto.DeliverymanRegisterDTO;
import com.supermall.takeout.dto.DeliverymanUpdateDTO;
import com.supermall.takeout.entity.Deliveryman;
import com.supermall.takeout.vo.DeliveryLoginVO;

public interface DeliverymanService {

    DeliveryLoginVO login(DeliverymanLoginDTO dto);

    /** 骑手自助注册：注册即启用，直接返回 token（注册即登录） */
    DeliveryLoginVO register(DeliverymanRegisterDTO dto);

    Deliveryman getById(Long id);

    void updateProfile(DeliverymanUpdateDTO dto);
}
