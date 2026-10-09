package com.supermall.mall.controller;

import com.supermall.context.BaseContext;
import com.supermall.entity.Address;
import com.supermall.mall.dto.MallAddressDTO;
import com.supermall.mall.service.MallAddressService;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城买家 - 收货地址。认证：请求头 token。
 *
 * <p>与外卖共用 {@code address} 表，所以在外卖里建的地址在这里也能看到、能设为默认。
 */
@RestController
@RequestMapping("/api/mall/user/address")
@Tag(name = "电商-收货地址")
@Slf4j
@RequiredArgsConstructor
public class MallUserAddressController {

    private final MallAddressService mallAddressService;

    @GetMapping
    @Operation(summary = "地址列表", description = "默认地址排最前，其余按最近修改倒序")
    public Result<List<Address>> list() {
        return Result.success(mallAddressService.list(BaseContext.getCurrentId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "地址详情")
    public Result<Address> detail(@PathVariable Long id) {
        return Result.success(mallAddressService.detail(BaseContext.getCurrentId(), id));
    }

    @PostMapping
    @Operation(summary = "新增地址",
            description = "第一条地址自动成为默认地址。省市区推荐传 areaCode（服务端回填名称）；也可直接传 province/city/district")
    public Result<Long> save(@Valid @RequestBody MallAddressDTO dto) {
        return Result.success(mallAddressService.save(BaseContext.getCurrentId(), dto));
    }

    @PutMapping
    @Operation(summary = "修改地址", description = "必须传 id；只能改自己的地址")
    public Result<Void> update(@Valid @RequestBody MallAddressDTO dto) {
        mallAddressService.update(BaseContext.getCurrentId(), dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址（幂等）")
    public Result<Void> remove(@PathVariable Long id) {
        mallAddressService.remove(BaseContext.getCurrentId(), id);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认地址", description = "会把该用户其他地址的默认标记清掉")
    public Result<Void> setDefault(@PathVariable Long id) {
        mallAddressService.setDefault(BaseContext.getCurrentId(), id);
        return Result.success();
    }
}
