package com.supermall.mall.controller;

import com.supermall.mall.dto.MallBannerDTO;
import com.supermall.mall.entity.MallBanner;
import com.supermall.mall.service.MallBannerService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运营后台 - 推荐位 / 轮播图。认证：请求头 mall-admin-token。
 */
@RestController
@RequestMapping("/api/mall/admin/banners")
@Tag(name = "电商后台-推荐位")
@Slf4j
@RequiredArgsConstructor
public class MallAdminBannerController {

    private final MallBannerService mallBannerService;

    @GetMapping
    @Operation(summary = "推荐位列表（按位置 / 状态过滤）")
    public Result<List<MallBanner>> list(@RequestParam(required = false) Integer position,
                                        @RequestParam(required = false) Integer status) {
        return Result.success(mallBannerService.list(position, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "推荐位详情")
    public Result<MallBanner> detail(@PathVariable Long id) {
        return Result.success(mallBannerService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增推荐位")
    public Result<Long> create(@Valid @RequestBody MallBannerDTO dto) {
        dto.setId(null);
        return Result.success(mallBannerService.save(dto));
    }

    @PutMapping
    @Operation(summary = "修改推荐位（支持生效时间窗）")
    public Result<Long> update(@Valid @RequestBody MallBannerDTO dto) {
        return Result.success(mallBannerService.save(dto));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "上/下线：status=1 上线 / 0 下线")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        mallBannerService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除推荐位")
    public Result<Void> remove(@PathVariable Long id) {
        mallBannerService.remove(id);
        return Result.success();
    }
}
