package com.supermall.mall.controller;

import com.supermall.mall.service.MallReviewService;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营后台 - 评价管理。认证：请求头 mall-admin-token。
 */
@RestController
@RequestMapping("/api/mall/admin/reviews")
@Tag(name = "电商-运营后台评价")
@Slf4j
@RequiredArgsConstructor
public class MallAdminReviewController {

    private final MallReviewService mallReviewService;

    @GetMapping("/page")
    @Operation(summary = "评价分页",
            description = "可按 productId / userId / rating 过滤；不传即查全部")
    public Result<PageResult> page(@RequestParam(required = false) Long productId,
                                   @RequestParam(required = false) Long userId,
                                   @RequestParam(required = false) Integer rating,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(mallReviewService.pageByAdmin(productId, userId, rating, page, pageSize));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评价（违规内容）", description = "删除后自动重算商品的评分 / 评价数 / 好评率")
    public Result<Void> delete(@PathVariable Long id) {
        mallReviewService.delete(id);
        return Result.success();
    }
}
