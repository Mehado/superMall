package com.supermall.mall.controller;

import com.supermall.mall.dto.MallOrderPageQueryDTO;
import com.supermall.mall.dto.MallShipDTO;
import com.supermall.mall.service.MallOrderService;
import com.supermall.mall.vo.MallOrderVO;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营后台 - 订单管理。认证：请求头 mall-admin-token。
 */
@RestController
@RequestMapping("/api/mall/admin/orders")
@Tag(name = "电商-运营后台订单")
@Slf4j
@RequiredArgsConstructor
public class MallAdminOrderController {

    private final MallOrderService mallOrderService;

    @GetMapping("/page")
    @Operation(summary = "订单分页",
            description = "可按 status / orderNumber / userId 过滤；不传 userId 即查全部订单")
    public Result<PageResult> page(MallOrderPageQueryDTO query) {
        return Result.success(mallOrderService.pageForAdmin(query));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "订单详情")
    public Result<MallOrderVO> detail(@PathVariable String orderNumber) {
        return Result.success(mallOrderService.detailForAdmin(orderNumber));
    }

    @PutMapping("/{orderNumber}/ship")
    @Operation(summary = "发货", description = "待发货 → 待收货，并写入物流公司 + 物流单号")
    public Result<Void> ship(@PathVariable String orderNumber, @Valid @RequestBody MallShipDTO dto) {
        mallOrderService.ship(orderNumber, dto);
        return Result.success();
    }
}
