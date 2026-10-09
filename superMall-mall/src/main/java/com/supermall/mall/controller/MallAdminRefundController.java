package com.supermall.mall.controller;

import com.supermall.mall.dto.MallRefundAuditDTO;
import com.supermall.mall.service.MallRefundService;
import com.supermall.mall.vo.MallRefundVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营后台 - 退款审核。认证：请求头 mall-admin-token。
 */
@RestController
@RequestMapping("/api/mall/admin/refunds")
@Tag(name = "电商-运营后台退款审核")
@Slf4j
@RequiredArgsConstructor
public class MallAdminRefundController {

    private final MallRefundService mallRefundService;

    @GetMapping("/page")
    @Operation(summary = "退款申请分页", description = "status 可选：PENDING / APPROVED / REJECTED")
    public Result<PageResult> page(@RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(mallRefundService.pageForAdmin(status, page, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "退款申请详情（含关联订单与明细）")
    public Result<MallRefundVO> detail(@PathVariable Long id) {
        return Result.success(mallRefundService.detailForAdmin(id));
    }

    @PutMapping("/{id}/audit")
    @Operation(summary = "审核退款",
            description = "approved=true 同意（订单→已退款、回补库存、扣回销量）；false 驳回（订单回到申请前状态）")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody MallRefundAuditDTO dto) {
        mallRefundService.audit(id, dto);
        return Result.success();
    }
}
