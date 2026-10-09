package com.supermall.mall.controller;

import com.supermall.context.BaseContext;
import com.supermall.mall.dto.MallAdminLoginDTO;
import com.supermall.mall.entity.MallAdmin;
import com.supermall.mall.service.MallAdminService;
import com.supermall.mall.vo.MallAdminLoginVO;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营后台接口。认证：请求头 mall-admin-token。
 *
 * <p>login 由拦截器放行，其余需要管理员登录态。
 * P1 起商品/分类/订单/会员/统计等后台接口都会挂在这个前缀下。
 */
@RestController
@RequestMapping("/api/mall/admin")
@Tag(name = "电商-运营后台")
@Slf4j
@RequiredArgsConstructor
public class MallAdminController {

    private final MallAdminService mallAdminService;

    @PostMapping("/login")
    @Operation(summary = "管理员登录")
    public Result<MallAdminLoginVO> login(@Valid @RequestBody MallAdminLoginDTO dto) {
        return Result.success(mallAdminService.login(dto));
    }

    @GetMapping("/profile")
    @Operation(summary = "查询当前管理员")
    public Result<MallAdmin> profile() {
        return Result.success(mallAdminService.getById(BaseContext.getCurrentId()));
    }
}
