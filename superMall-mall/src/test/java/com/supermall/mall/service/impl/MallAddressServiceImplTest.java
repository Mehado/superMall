package com.supermall.mall.service.impl;

import com.supermall.entity.Address;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallAddressDTO;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallAddressMapper;
import com.supermall.mall.service.MallAreaService;
import com.supermall.mall.vo.MallAreaResolvedVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 收货地址：默认地址规则、区划回填、越权防护、数量上限。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallAddressServiceImpl - 收货地址")
class MallAddressServiceImplTest {

    private static final Long USER = 7L;

    @Mock private MallAddressMapper addressMapper;
    @Mock private MallAreaService areaService;

    private MallAddressServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MallAddressServiceImpl(addressMapper, areaService);
        lenient().when(areaService.resolve("110101")).thenReturn(MallAreaResolvedVO.builder()
                .areaCode("110101").province("北京市").city("北京市").district("东城区")
                .fullName("北京市 北京市 东城区").build());
    }

    private MallAddressDTO dto(String areaCode) {
        MallAddressDTO dto = new MallAddressDTO();
        dto.setConsignee("张三");
        dto.setPhone("13800138000");
        dto.setAreaCode(areaCode);
        dto.setDetail("某某路 1 号");
        return dto;
    }

    @Test
    @DisplayName("新增：第一条地址自动成为默认，并回填区划名称")
    void saveFirstBecomesDefault() {
        when(addressMapper.countByUserId(USER)).thenReturn(0);

        service.save(USER, dto("110101"));

        verify(addressMapper).clearDefault(USER);
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(captor.capture());
        Address saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER);
        assertThat(saved.getIsDefault()).isEqualTo(1);
        assertThat(saved.getAreaCode()).isEqualTo("110101");
        assertThat(saved.getProvince()).isEqualTo("北京市");
        assertThat(saved.getDistrict()).isEqualTo("东城区");
    }

    @Test
    @DisplayName("新增：非第一条且传 isDefault=1 时先清掉旧默认")
    void saveExplicitDefaultClearsOthers() {
        when(addressMapper.countByUserId(USER)).thenReturn(3);
        MallAddressDTO dto = dto("110101");
        dto.setIsDefault(1);

        service.save(USER, dto);

        verify(addressMapper).clearDefault(USER);
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isEqualTo(1);
    }

    @Test
    @DisplayName("新增：不传 isDefault 且已有地址 → 非默认，且不清旧默认")
    void saveNonDefault() {
        when(addressMapper.countByUserId(USER)).thenReturn(2);

        service.save(USER, dto("110101"));

        verify(addressMapper, never()).clearDefault(anyLong());
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isEqualTo(0);
    }

    @Test
    @DisplayName("新增：超过 20 条被拒")
    void saveLimit() {
        when(addressMapper.countByUserId(USER)).thenReturn(20);
        assertThatThrownBy(() -> service.save(USER, dto("110101")))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.ADDRESS_LIMIT);
        verify(addressMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新增：不传 areaCode 且省市区不全 → 拒绝")
    void saveRequiresArea() {
        when(addressMapper.countByUserId(USER)).thenReturn(0);
        MallAddressDTO dto = dto(null);
        dto.setProvince("北京市");

        assertThatThrownBy(() -> service.save(USER, dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.ADDRESS_AREA_REQUIRED);
    }

    @Test
    @DisplayName("新增：不传 areaCode 但省市区齐全 → 允许手工录入")
    void saveManualArea() {
        when(addressMapper.countByUserId(USER)).thenReturn(0);
        MallAddressDTO dto = dto(null);
        dto.setProvince("北京市");
        dto.setCity("北京市");
        dto.setDistrict("东城区");

        service.save(USER, dto);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(captor.capture());
        assertThat(captor.getValue().getAreaCode()).isNull();
        assertThat(captor.getValue().getDistrict()).isEqualTo("东城区");
    }

    @Test
    @DisplayName("修改：只能改自己的地址")
    void updateRejectsOthers() {
        when(addressMapper.getById(5L)).thenReturn(Address.builder().id(5L).userId(999L).build());
        MallAddressDTO dto = dto("110101");
        dto.setId(5L);

        assertThatThrownBy(() -> service.update(USER, dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.ADDRESS_NOT_FOUND);
        verify(addressMapper, never()).update(any());
    }

    @Test
    @DisplayName("修改：原来是默认地址，编辑时不传 isDefault 也应保持默认")
    void updateKeepsDefault() {
        when(addressMapper.getById(5L)).thenReturn(Address.builder().id(5L).userId(USER).isDefault(1).build());
        MallAddressDTO dto = dto("110101");
        dto.setId(5L);

        service.update(USER, dto);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).update(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isEqualTo(1);
    }

    @Test
    @DisplayName("设为默认：先清后设")
    void setDefault() {
        when(addressMapper.getById(5L)).thenReturn(Address.builder().id(5L).userId(USER).build());

        service.setDefault(USER, 5L);

        verify(addressMapper).clearDefault(USER);
        verify(addressMapper).setDefault(5L, USER);
    }

    @Test
    @DisplayName("删除是幂等的")
    void removeIdempotent() {
        service.remove(USER, 5L);
        verify(addressMapper).deleteById(5L, USER);
    }

    @Test
    @DisplayName("requireOwned：不存在 / 别人的都抛业务异常")
    void requireOwned() {
        when(addressMapper.getById(5L)).thenReturn(Address.builder().id(5L).userId(USER).build());
        assertThat(service.requireOwned(USER, 5L).getId()).isEqualTo(5L);

        assertThatThrownBy(() -> service.requireOwned(USER, 6L))
                .isInstanceOf(MallBusinessException.class);
        assertThatThrownBy(() -> service.requireOwned(USER, null))
                .isInstanceOf(MallBusinessException.class);
    }
}
