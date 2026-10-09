package com.supermall.takeout.controller.user;

import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.PasswordEditDTO;
import com.supermall.dto.UserLoginDTO;
import com.supermall.dto.UserRegisterDTO;
import com.supermall.takeout.dto.UserUpdateDTO;
import com.supermall.entity.User;
import com.supermall.result.Result;
import com.supermall.takeout.service.UserService;
import com.supermall.vo.UserLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户端-账号")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册（成功即返回 token，无需再登录一次）")
    public Result<UserLoginVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @GetMapping("/profile")
    @Operation(summary = "查询个人资料")
    public Result<User> profile() {
        User user = userService.getById(BaseContext.getCurrentId());
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @PutMapping("/profile")
    @Operation(summary = "修改个人资料")
    public Result<Void> updateProfile(@Valid @RequestBody UserUpdateDTO dto) {
        userService.updateProfile(dto);
        return Result.success();
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<Void> editPassword(@Valid @RequestBody PasswordEditDTO dto) {
        userService.editPassword(dto);
        return Result.success();
    }
}
