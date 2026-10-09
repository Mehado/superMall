package com.supermall.takeout.controller.user;

import com.supermall.takeout.dto.AddressDTO;
import com.supermall.entity.Address;
import com.supermall.result.Result;
import com.supermall.takeout.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/address")
@Tag(name = "用户端-收货地址")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "地址列表")
    public Result<List<Address>> list() {
        return Result.success(addressService.list());
    }

    @PostMapping
    @Operation(summary = "新增地址")
    public Result<Void> save(@Valid @RequestBody AddressDTO dto) {
        addressService.save(dto);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改地址")
    public Result<Void> update(@Valid @RequestBody AddressDTO dto) {
        addressService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "地址详情")
    public Result<Address> detail(@PathVariable Long id) {
        return Result.success(addressService.getById(id));
    }

    @PutMapping("/default/{id}")
    @Operation(summary = "设为默认地址")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return Result.success();
    }
}
