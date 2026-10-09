package com.supermall.mall.controller;

import com.supermall.context.BaseContext;
import com.supermall.dto.UserLoginDTO;
import com.supermall.dto.UserRegisterDTO;
import com.supermall.entity.User;
import com.supermall.mall.dto.MallUserProfileDTO;
import com.supermall.mall.service.MallUserService;
import com.supermall.result.Result;
import com.supermall.vo.UserLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城买家接口。认证：请求头 token（与外卖买家同一套 token）。
 *
 * <p>login / register 由拦截器放行，其余需要登录。
 */
@RestController
@RequestMapping("/api/mall/user")
@Tag(name = "电商-买家账号")
@Slf4j
@RequiredArgsConstructor
public class MallUserController {

    private final MallUserService mallUserService;

    @PostMapping("/register")
    @Operation(summary = "买家注册（成功即返回 token，无需再登录一次）")
    public Result<UserLoginVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        return Result.success(mallUserService.register(dto));
    }

    @PostMapping("/login")
    @Operation(summary = "买家登录")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return Result.success(mallUserService.login(dto));
    }

    @GetMapping("/profile")
    @Operation(summary = "查询买家资料")
    public Result<User> profile() {
        return Result.success(mallUserService.getById(BaseContext.getCurrentId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "修改买家资料（昵称 / 手机号 / 头像）",
            description = "只传想改的字段；头像先调 POST /api/mall/user/file/avatar 上传拿 URL，空串表示清除头像")
    public Result<Void> updateProfile(@Valid @RequestBody MallUserProfileDTO dto) {
        mallUserService.updateProfile(BaseContext.getCurrentId(), dto);
        return Result.success();
    }
}
