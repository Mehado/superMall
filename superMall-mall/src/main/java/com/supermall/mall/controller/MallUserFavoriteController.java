package com.supermall.mall.controller;

import com.supermall.context.BaseContext;
import com.supermall.mall.service.MallFavoriteService;
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

import java.util.Map;

/**
 * 商城买家 - 收藏夹。认证：请求头 token。
 *
 * <p>userId 一律从登录态取（{@code BaseContext}），不接受前端传 —— 否则能看别人的收藏夹。
 */
@RestController
@RequestMapping("/api/mall/user/favorites")
@Tag(name = "电商-收藏夹")
@Slf4j
@RequiredArgsConstructor
public class MallUserFavoriteController {

    private final MallFavoriteService mallFavoriteService;

    @PostMapping("/{productId}")
    @Operation(summary = "收藏商品（幂等：重复收藏不报错）")
    public Result<Void> add(@PathVariable Long productId) {
        mallFavoriteService.add(BaseContext.getCurrentId(), productId);
        return Result.success();
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "取消收藏（幂等：没收藏过也不报错）")
    public Result<Void> remove(@PathVariable Long productId) {
        mallFavoriteService.remove(BaseContext.getCurrentId(), productId);
        return Result.success();
    }

    @GetMapping("/{productId}/exists")
    @Operation(summary = "是否已收藏（商品详情页用）")
    public Result<Map<String, Boolean>> exists(@PathVariable Long productId) {
        boolean exists = mallFavoriteService.exists(BaseContext.getCurrentId(), productId);
        return Result.success(Map.of("favorited", exists));
    }

    @GetMapping("/count")
    @Operation(summary = "收藏总数")
    public Result<Map<String, Integer>> count() {
        return Result.success(Map.of("total", mallFavoriteService.count(BaseContext.getCurrentId())));
    }

    @GetMapping
    @Operation(summary = "收藏列表（商品卡片，按收藏时间倒序；含已下架商品，status=0 需标灰）")
    public Result<PageResult> page(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(mallFavoriteService.page(BaseContext.getCurrentId(), page, pageSize));
    }
}
