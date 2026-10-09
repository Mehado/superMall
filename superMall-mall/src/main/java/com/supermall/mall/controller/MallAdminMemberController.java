package com.supermall.mall.controller;

import com.supermall.mall.service.MallMemberService;
import com.supermall.mall.vo.MallMemberVO;
import com.supermall.result.PageResult;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营后台 - 会员管理。认证：请求头 mall-admin-token。
 */
@RestController
@RequestMapping("/api/mall/admin/members")
@Tag(name = "电商-运营后台会员")
@Slf4j
@RequiredArgsConstructor
public class MallAdminMemberController {

    private final MallMemberService mallMemberService;

    @GetMapping("/page")
    @Operation(summary = "会员分页",
            description = "keyword 模糊匹配 邮箱 / 昵称 / 手机号；status：1 启用 0 禁用。返回带订单数 / 消费额 / 评价数")
    public Result<PageResult> page(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(mallMemberService.page(keyword, status, page, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "会员详情（含统计）")
    public Result<MallMemberVO> detail(@PathVariable Long id) {
        return Result.success(mallMemberService.detail(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用 / 禁用会员",
            description = "status=1 启用，0 禁用；禁用后该账号在外卖与电商都无法登录（同一账号）")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        mallMemberService.updateStatus(id, status);
        return Result.success();
    }
}
