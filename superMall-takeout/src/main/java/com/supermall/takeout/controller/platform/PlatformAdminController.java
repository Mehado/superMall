package com.supermall.takeout.controller.platform;

import com.supermall.context.BaseContext;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import com.supermall.takeout.dto.MerchantAuditDTO;
import com.supermall.takeout.dto.PlatformAdminLoginDTO;
import com.supermall.takeout.dto.PlatformAdminProfileDTO;
import com.supermall.takeout.entity.PlatformAdmin;
import com.supermall.takeout.service.PlatformAdminService;
import com.supermall.takeout.vo.PlatformAdminLoginVO;
import com.supermall.takeout.vo.PlatformOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台运营端（外卖）：商家入驻审核 + 骑手管理。认证：请求头 {@code platform-token}。
 *
 * <p>平台方是外卖板块的"第四端"，与商家 / 骑手 / 买家三端账号完全隔离。
 */
@RestController
@RequestMapping("/api/platform")
@Tag(name = "平台运营端-入驻审核与骑手管理")
@RequiredArgsConstructor
public class PlatformAdminController {

    private final PlatformAdminService platformAdminService;

    @PostMapping("/login")
    @Operation(summary = "平台管理员登录")
    public Result<PlatformAdminLoginVO> login(@Valid @RequestBody PlatformAdminLoginDTO dto) {
        return Result.success(platformAdminService.login(dto));
    }

    /* ---------------- 我的资料 ---------------- */

    @GetMapping("/profile")
    @Operation(summary = "查询平台管理员资料", description = "password 恒为 null")
    public Result<PlatformAdmin> profile() {
        return Result.success(platformAdminService.getById(BaseContext.getCurrentId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "修改平台管理员资料（姓名）")
    public Result<Void> updateProfile(@Valid @RequestBody PlatformAdminProfileDTO dto) {
        platformAdminService.updateProfile(BaseContext.getCurrentId(), dto);
        return Result.success();
    }

    /* ---------------- 看板 ---------------- */

    @GetMapping("/overview")
    @Operation(summary = "平台看板：待审商家 / 商家总数 / 骑手数")
    public Result<PlatformOverviewVO> overview() {
        return Result.success(platformAdminService.overview());
    }

    /* ---------------- 商家入驻审核 ---------------- */

    @GetMapping("/merchants/page")
    @Operation(summary = "商家入驻申请分页",
            description = "auditStatus：0 待审核 1 已通过 2 已驳回；不传=全部（待审核排前面）。keyword 匹配店铺名/邮箱/手机号")
    public Result<PageResult> merchants(@RequestParam(required = false) Integer auditStatus,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer pageSize) {
        return Result.success(platformAdminService.pageMerchants(auditStatus, keyword, page, pageSize));
    }

    @PutMapping("/merchants/{id}/audit")
    @Operation(summary = "审核入驻申请",
            description = "approved=true 通过（店铺可直接营业）；approved=false 驳回（必须填 reason）")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody MerchantAuditDTO dto) {
        platformAdminService.auditMerchant(id, dto);
        return Result.success();
    }

    /* ---------------- 骑手管理 ---------------- */

    @GetMapping("/deliverymen/page")
    @Operation(summary = "骑手分页", description = "keyword 匹配姓名/邮箱/手机号；status：1 可接单 0 停用")
    public Result<PageResult> deliverymen(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer pageSize) {
        return Result.success(platformAdminService.pageDeliverymen(keyword, status, page, pageSize));
    }

    @PutMapping("/deliverymen/{id}/status")
    @Operation(summary = "启用 / 停用骑手：status=1 可接单 / 0 停用")
    public Result<Void> updateDeliverymanStatus(@PathVariable Long id, @RequestParam Integer status) {
        platformAdminService.updateDeliverymanStatus(id, status);
        return Result.success();
    }
}
