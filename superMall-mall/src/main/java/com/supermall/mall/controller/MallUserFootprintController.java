package com.supermall.mall.controller;

import com.supermall.context.BaseContext;
import com.supermall.mall.service.MallFootprintService;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城买家 - 浏览足迹。认证：请求头 token。
 *
 * <p>商品详情页打开后调一次 {@code POST /favorites/{productId}} 记录；
 * 未登录时不记（无法归属到人），前端判断已登录再调即可。
 */
@RestController
@RequestMapping("/api/mall/user/footprints")
@Tag(name = "电商-浏览足迹")
@Slf4j
@RequiredArgsConstructor
public class MallUserFootprintController {

    private final MallFootprintService mallFootprintService;

    @PostMapping("/{productId}")
    @Operation(summary = "记录一次浏览（幂等去重：同一商品只留一行，累加浏览次数）")
    public Result<Void> record(@PathVariable Long productId) {
        mallFootprintService.record(BaseContext.getCurrentId(), productId);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "足迹列表（按最近浏览倒序，含商品卡片）")
    public Result<PageResult> page(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(mallFootprintService.page(BaseContext.getCurrentId(), page, pageSize));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "删除单条足迹")
    public Result<Void> removeOne(@PathVariable Long productId) {
        mallFootprintService.removeOne(BaseContext.getCurrentId(), productId);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "清空足迹")
    public Result<Void> clear() {
        mallFootprintService.clear(BaseContext.getCurrentId());
        return Result.success();
    }
}
