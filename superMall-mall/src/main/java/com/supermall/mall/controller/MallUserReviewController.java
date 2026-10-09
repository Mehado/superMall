package com.supermall.mall.controller;

import com.supermall.context.BaseContext;
import com.supermall.mall.dto.MallReviewFollowDTO;
import com.supermall.mall.dto.MallReviewSubmitDTO;
import com.supermall.mall.service.MallReviewService;
import com.supermall.mall.vo.MallOrderReviewVO;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城买家 - 评价。认证：请求头 token。
 */
@RestController
@RequestMapping("/api/mall/user/reviews")
@Tag(name = "电商-买家评价")
@Slf4j
@RequiredArgsConstructor
public class MallUserReviewController {

    private final MallReviewService mallReviewService;

    @PostMapping
    @Operation(summary = "批量评价订单",
            description = "只有已完成订单能评价；粒度是订单明细，一个明细只能评一次")
    public Result<Integer> submit(@Valid @RequestBody MallReviewSubmitDTO dto) {
        return Result.success(mallReviewService.submit(BaseContext.getCurrentId(), dto));
    }

    @PostMapping("/{id}/follow")
    @Operation(summary = "追评", description = "一次评价只能追评一次；追评不改变评分")
    public Result<Void> follow(@PathVariable Long id, @Valid @RequestBody MallReviewFollowDTO dto) {
        mallReviewService.follow(BaseContext.getCurrentId(), id, dto);
        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary = "我的评价分页（含商品快照与晒图）")
    public Result<PageResult> page(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(mallReviewService.pageMine(BaseContext.getCurrentId(), page, pageSize));
    }

    @GetMapping("/order/{orderNumber}")
    @Operation(summary = "某订单的评价情况",
            description = "返回每个明细是否已评（review 为 null 表示未评）；canReview 为是否可评价")
    public Result<MallOrderReviewVO> orderReviews(@PathVariable String orderNumber) {
        return Result.success(mallReviewService.orderReviews(BaseContext.getCurrentId(), orderNumber));
    }
}
